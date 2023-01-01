package cz.rblaha15.odecty

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.net.URI

@Serializer(forClass = File::class)
@OptIn(ExperimentalSerializationApi::class)
object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.toURI().toASCIIString())
    override fun deserialize(decoder: Decoder) = File(URI(decoder.decodeString()))
}

@Serializable
internal data class Vec(
    val kategorie: Kategorie,
    val nazev: String? = null,
    val jednotky: String,
    val mnozstvi: String = "",
    val maMitFotku: Boolean = false,
    @Serializable(with = FileSerializer::class)
    val fotka: File? = null,
) {

    enum class Kategorie(val nazev: String? = null) {
        Plyn, Elektrina("Elektřina"), Voda, TC("TČ"), Auto;
    }

    companion object {
        fun vse(): List<Vec> = listOf(
            Vec(kategorie = Kategorie.Plyn, jednotky = "m³"),
            Vec(kategorie = Kategorie.Elektrina, nazev = "Nízký tarif", jednotky = "kWh"),
            Vec(kategorie = Kategorie.Elektrina, nazev = "Vysoký tarif", jednotky = "kWh"),
            Vec(kategorie = Kategorie.Voda, nazev = "Řád", jednotky = "m³", maMitFotku = true),
            Vec(kategorie = Kategorie.Voda, nazev = "Studna", jednotky = "m³", maMitFotku = true),
            Vec(kategorie = Kategorie.TC, nazev = "Spotřeba", jednotky = "kWh"),
            Vec(kategorie = Kategorie.TC, nazev = "Výroba", jednotky = "kWh"),
            Vec(kategorie = Kategorie.Auto, jednotky = "km"),
        )
    }
}