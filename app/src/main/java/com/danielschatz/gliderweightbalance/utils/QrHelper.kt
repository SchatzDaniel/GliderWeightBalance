package com.danielschatz.gliderweightbalance.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.danielschatz.gliderweightbalance.data.model.Aircraft
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.danielschatz.gliderweightbalance.data.model.PayloadStation
import com.danielschatz.gliderweightbalance.data.model.StationWithPresets
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QrHelper {

    data class CompactProfile(
        @SerializedName("t") val type: String,
        @SerializedName("r") val reg: String,
        @SerializedName("m") val maxM: Double?,
        @SerializedName("n") val maxN: Double?,
        @SerializedName("c1") val minC: Double?,
        @SerializedName("c2") val maxC: Double?,
        @SerializedName("ew") val eW: Double?,
        @SerializedName("ea") val eA: Double?,
        @SerializedName("fm") val fM: Double?,
        @SerializedName("sm") val sM: Double?,
        @SerializedName("wa") val wA: Double?,
        @SerializedName("s") val st: List<CompactStation>
    )

    data class CompactStation(
        @SerializedName("n") val n: String,
        @SerializedName("a") val a: Double,
        @SerializedName("m") val m: Double?,
        @SerializedName("u") val u: String?,
        @SerializedName("nl") val nl: Boolean,
        @SerializedName("c") val c: Boolean,
        @SerializedName("ft") val ft: String?
    )

    fun toQrString(profile: AircraftProfile): String {
        val compact = CompactProfile(
            type = profile.aircraft.aircraftType,
            reg = profile.aircraft.registration,
            maxM = profile.aircraft.maxTotalMass,
            maxN = profile.aircraft.maxNonLiftingMass,
            minC = profile.aircraft.minCg,
            maxC = profile.aircraft.maxCg,
            eW = profile.aircraft.emptyWeight,
            eA = profile.aircraft.emptyWeightArm,
            fM = profile.aircraft.fuselageMass,
            sM = profile.aircraft.stabilizerMass,
            wA = profile.aircraft.wingArea,
            st = profile.stations.map {
                CompactStation(it.station.name, it.station.arm, it.station.maxMass, it.station.unit, it.station.isNonLifting, it.station.isConsumable, it.station.fluidType)
            }
        )
        return "GWB:" + Gson().toJson(compact)
    }

    fun fromQrString(qrString: String): AircraftProfile? {
        if (!qrString.startsWith("GWB:")) return null
        return try {
            val json = qrString.substring(4)
            val compact = Gson().fromJson(json, CompactProfile::class.java) ?: return null
            
            val aircraft = Aircraft(
                aircraftType = compact.type,
                registration = compact.reg,
                maxTotalMass = compact.maxM,
                maxNonLiftingMass = compact.maxN,
                minCg = compact.minC,
                maxCg = compact.maxC,
                emptyWeight = compact.eW,
                emptyWeightArm = compact.eA,
                fuselageMass = compact.fM,
                stabilizerMass = compact.sM,
                wingArea = compact.wA
            )
            
            val stations = compact.st.map {
                StationWithPresets(
                    station = PayloadStation(0, 0, it.n, it.a, it.m, it.u, 0, it.nl, null, false, false, false, null, 1, it.ft, it.c),
                    presets = emptyList()
                )
            }
            AircraftProfile(aircraft, stations)
        } catch (_: Exception) { null }
    }

    fun generateQrBitmap(content: String): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 4,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 700, 700, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Using explicit bitMatrix.get to avoid conflict with other 'get' methods
                val isBlack = bitMatrix.get(x, y)
                bitmap[x, y] = if (isBlack) Color.BLACK else Color.WHITE
            }
        }
        return bitmap
    }
}
