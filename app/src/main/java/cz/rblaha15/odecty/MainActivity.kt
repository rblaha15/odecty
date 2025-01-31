package cz.rblaha15.odecty

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import cz.rblaha15.odecty.ui.theme.OdectyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH

private lateinit var fotkyAPI: FotkyAPI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fotkyAPI = FotkyAPIImpl(this)
        lifecycle.addObserver(fotkyAPI)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        setContent {
            OdectyTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainContent(VsechnoVecar(this))
                }
            }
        }
    }
}

private class VsechnoVecar(ctx: Context) {

    private val sharedPrefs = ctx.getSharedPreferences("AGRHH", Context.MODE_PRIVATE)
    var vsechnyVeci: List<Vec>
        get() {
            val json = sharedPrefs.getString("vse", null) ?: return Vec.vse()
            return Json.decodeFromString(json)
        }
        set(vse) {
            val json = Json.encodeToString(vse)
            sharedPrefs.edit {
                putString("vse", json)
            }
        }
    var posledniVeci: List<Vec>
        get() {
            val json = sharedPrefs.getString("posl", null) ?: return Vec.vse()
            return Json.decodeFromString(json)
        }
        set(vse) {
            val json = Json.encodeToString(vse)
            sharedPrefs.edit {
                putString("posl", json)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    vsechnoVecar: VsechnoVecar,
) {

    var vsechnyVeci by remember { mutableStateOf(vsechnoVecar.vsechnyVeci) }
    LaunchedEffect(vsechnyVeci) {
        vsechnoVecar.vsechnyVeci = vsechnyVeci
    }
    var posledniVeci by remember { mutableStateOf(vsechnoVecar.posledniVeci) }
    LaunchedEffect(posledniVeci) {
        vsechnoVecar.posledniVeci = posledniVeci
    }

    var chyba by remember { mutableStateOf("") }
    var poslanoDialog by remember { mutableStateOf(false) }
    if (poslanoDialog) AlertDialog(
        onDismissRequest = {
            poslanoDialog = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    poslanoDialog = false
                }
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {},
        title = {
            Text(if (chyba.isEmpty()) "Emaily odeslány!" else "Emaily neodeslány")
        },
        text = {
            Text(if (chyba.isEmpty()) "Emaily se úspěšně odeslaly na adresu Hana.Reznickova@regulus.cz a blahova@regulus.cz" else "Bohužel, oba emaily se nepodařilo odeslat. Zde je chyba: $chyba")
        },
    )

    var loadingDialog by remember { mutableStateOf(false) }
    if (loadingDialog) AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {},
        title = {
            Text("Odesílání")
        },
        text = {
            Text("Emaily se odesílají. Doba odesílání závisí na rychlosti připojení k internetu a na počtu přiložených fotek")
        },
    )

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val odeslatMaily = block@{ ->
        if (vsechnyVeci.any { it.mnozstvi.isBlank() }) {
            Toast.makeText(context, "Nejsou vyplněny všechny údaje!", Toast.LENGTH_LONG).show()
            return@block
        }
        scope.launch(Dispatchers.IO) {
            loadingDialog = true
            val api = MailAPIImpl()

            var prvni: String? = null
            var druhy: String? = null

            api.sendEmail(
                body = vsechnyVeci
                    .filter { it.kategorie != Vec.Kategorie.Auto }
                    .groupBy { it.kategorie }.toList()
                    .joinToString("\n") { (kategorie, veci) ->
                        (kategorie.nazev ?: kategorie.name) + ":\n" + veci.joinToString("\n") {
                            it.run {
                                if (nazev != null)
                                    "$nazev – $mnozstvi $jednotky"
                                else
                                    "$mnozstvi $jednotky"
                            }
                        }
                    },
                to = listOf("radek.blaha.15@gmail.com"),
//                to = listOf("blahova@regulus.cz"),
//                cc = listOf("roman.blaha.cb@gmail.com"),
                subject = "Odečty",
                attachments = vsechnyVeci
                    .filter { it.kategorie != Vec.Kategorie.Auto }
                    .mapNotNull { it.fotka },
                onError = {
                    prvni = it.message ?: "?"
                },
                onSend = {
                    prvni = ""
                }
            )

            api.sendEmail(
                body = """
                            Ahoj Hanko,
                            
                            stav k ${Calendar.getInstance().let { "${it[DAY_OF_MONTH]}. ${it[MONTH] + 1}" }}:
                            ${vsechnyVeci.first { it.kategorie == Vec.Kategorie.Auto }.mnozstvi} km
                            
                            Roman Bláha
                        """.trimIndent(),
                to = listOf("radek.blaha.15@gmail.com"),
//                to = listOf("Hana.Reznickova@regulus.cz"),
//                cc = listOf("blaha@regulus.cz"),
                subject = "km",
                attachments = vsechnyVeci
                    .filter { it.kategorie == Vec.Kategorie.Auto }
                    .mapNotNull { it.fotka },
                onError = {
                    druhy = it.message ?: "?"
                },
                onSend = {
                    druhy = ""
                }
            )

            while (prvni == null || druhy == null) Unit

            loadingDialog = false

            chyba = when {
                prvni == "" && druhy == "" -> ""
                prvni == "" -> druhy
                druhy == "" -> prvni
                else -> "$prvni, $druhy"
            }
            poslanoDialog = true

            if (chyba == "") {
                posledniVeci = vsechnyVeci
                vsechnyVeci = Vec.vse()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Odečty") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    odeslatMaily()
                },
            ) {
                Icon(Icons.AutoMirrored.Default.Send, "Odeslat email")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            vsechnyVeci.groupBy { it.kategorie }.forEach { (kategorie, veci) ->

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp)
                ) {

                    Text(
                        text = kategorie.nazev ?: kategorie.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )

                    veci.forEach { vec ->

                        val minulaVec = posledniVeci.find { it.kategorie == vec.kategorie && it.nazev == vec.nazev }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(all = 8.dp)
                        ) {

                            if (vec.nazev != null) Text(vec.nazev + ":")
                            val focusManager = LocalFocusManager.current

                            TextField(
                                value = vec.mnozstvi,
                                onValueChange = { mnozstvi ->
                                    try {
                                        vsechnyVeci = vsechnyVeci.mutate {
                                            it[it.indexOf(vec)] = vec.copy(mnozstvi = mnozstvi.replace('.', ','))
                                        }
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1F),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions {
                                    focusManager.moveFocus(focusDirection = FocusDirection.Down)
                                },
                                supportingText = {
                                    if (minulaVec != null) Text("${minulaVec.mnozstvi} ${minulaVec.jednotky}")
                                }
                            )

                            Text(
                                text = vec.jednotky,
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            if (vec.maMitFotku) {
                                if (vec.fotka == null) IconButton(
                                    onClick = {
                                        fotkyAPI.vyfotitFotku(vec.nazev ?: kategorie.nazev ?: "?") { file ->
                                            vsechnyVeci = vsechnyVeci.mutate {
                                                it[indexOf(vec)] = vec.copy(fotka = file)
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, "Přidat fotku")
                                }
                                else IconButton(
                                    onClick = {
                                        vsechnyVeci = vsechnyVeci.mutate {
                                            it[indexOf(vec)] = vec.copy(fotka = null)
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.HideImage, "Odstranit fotku")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

fun <T> List<T>.mutate(block: MutableList<T>.(MutableList<T>) -> Unit) = buildList {
    addAll(this@mutate)
    this.block(this)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OdectyTheme(false) {
        val ctx = LocalContext.current
        MainContent(VsechnoVecar(ctx))
    }
}