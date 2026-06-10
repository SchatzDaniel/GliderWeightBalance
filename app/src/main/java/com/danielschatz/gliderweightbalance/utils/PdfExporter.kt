package com.danielschatz.gliderweightbalance.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.danielschatz.gliderweightbalance.R
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class PdfExporter(private val context: Context) {

    fun generateReport(
        profile: AircraftProfile,
        totalMass: Double,
        totalMoment: Double,
        nonLiftingMass: Double,
        cgLocation: Double,
        cgMac: Double?,
        isWithinLimits: Boolean
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.BLACK
        }
        val normalPaint = Paint().apply {
            textSize = 9f
            color = Color.BLACK
        }
        val statusPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }

        var y = 50f
        val margin = 40f

        // Title
        canvas.drawText(context.getString(R.string.pdf_report_title), margin, y, titlePaint)
        y += 40f

        // Metadata
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        canvas.drawText("${context.getString(R.string.pdf_date)} ${dateFormat.format(now)}", margin, y, normalPaint)
        y += 15f
        canvas.drawText("${context.getString(R.string.pdf_time)} ${timeFormat.format(now)}", margin, y, normalPaint)
        y += 25f

        canvas.drawText("${context.getString(R.string.pdf_registration)} ${profile.aircraft.registration}", margin, y, headerPaint)
        y += 15f
        canvas.drawText("${context.getString(R.string.pdf_type)} ${profile.aircraft.aircraftType}", margin, y, headerPaint)
        y += 40f

        // Table Headers
        val col1 = margin
        val col2 = 170f // Masse
        val col3 = 260f // n.t. Teile
        val col4 = 350f // Arm
        val col5 = 450f // Moment

        canvas.drawText(context.getString(R.string.pdf_station_name), col1, y, headerPaint)
        canvas.drawText(context.getString(R.string.pdf_mass, "kg/L"), col2, y, headerPaint)
        canvas.drawText(context.getString(R.string.pdf_non_lifting), col3, y, headerPaint)
        canvas.drawText(context.getString(R.string.pdf_arm), col4, y, headerPaint)
        canvas.drawText(context.getString(R.string.pdf_moment), col5, y, headerPaint)
        y += 5f
        canvas.drawLine(margin, y, 555f, y, normalPaint)
        y += 15f

        // Table Content: 1. Leermasse
        val emptyWeight = profile.aircraft.emptyWeight ?: 0.0
        val emptyArm = profile.aircraft.emptyWeightArm ?: 0.0
        val emptyMoment = emptyWeight * emptyArm
        
        canvas.drawText(context.getString(R.string.Leermasse), col1, y, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "%.1f", emptyWeight), col2, y, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "%.1f", emptyArm), col4, y, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", emptyMoment), col5, y, normalPaint)
        y += 15f

        // 1a. Rumpf (Bestandteil der Leermasse)
        profile.aircraft.fuselageMass?.let {
            canvas.drawText(" - ${context.getString(R.string.pdf_label_fuselage)}", col1, y, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", it), col3, y, normalPaint)
            y += 15f
        }
        // 1b. HLW (Bestandteil der Leermasse)
        profile.aircraft.stabilizerMass?.let {
            canvas.drawText(" - ${context.getString(R.string.pdf_label_stabilizer)}", col1, y, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", it), col3, y, normalPaint)
            y += 15f
        }

        // Table Content: Stations (Payload)
        profile.sortedStations.forEach { station ->
            val mass = station.defaultValue ?: 0.0
            val moment = mass * station.arm
            
            canvas.drawText(station.name, col1, y, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", mass), col2, y, normalPaint)
            if (station.isNonLifting) {
                canvas.drawText(String.format(Locale.getDefault(), "%.1f", mass), col3, y, normalPaint)
            }
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", station.arm), col4, y, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.0f", moment), col5, y, normalPaint)
            y += 15f
        }

        y += 10f
        canvas.drawLine(margin, y, 555f, y, normalPaint)
        y += 30f

        // Summary
        canvas.drawText(context.getString(R.string.pdf_summary), margin, y, headerPaint)
        y += 20f

        canvas.drawText("${context.getString(R.string.pdf_total_mass)} ${String.format(Locale.getDefault(), "%.1f kg", totalMass)}", margin, y, normalPaint)
        y += 15f
        canvas.drawText("${context.getString(R.string.pdf_total_non_lifting)} ${String.format(Locale.getDefault(), "%.1f kg", nonLiftingMass)}", margin, y, normalPaint)
        y += 15f
        canvas.drawText("${context.getString(R.string.pdf_total_moment)} ${String.format(Locale.getDefault(), "%.0f kg·mm", totalMoment)}", margin, y, normalPaint)
        y += 15f
        canvas.drawText("${context.getString(R.string.pdf_cg_location)} ${String.format(Locale.getDefault(), "%.1f mm", cgLocation)}", margin, y, normalPaint)
        
        if (cgMac != null) {
            y += 15f
            canvas.drawText("${context.getString(R.string.pdf_cg_mac)} ${String.format(Locale.getDefault(), "%.1f %%", cgMac)}", margin, y, normalPaint)
        }
        
        y += 35f
        
        // Status
        statusPaint.color = if (isWithinLimits) "#4CAF50".toColorInt() else Color.RED
        val statusText = if (isWithinLimits) context.getString(R.string.status_ok) else context.getString(R.string.status_out_of_limits)
        canvas.drawText("${context.getString(R.string.pdf_status)} $statusText", margin, y, statusPaint)

        pdfDocument.finishPage(page)

        // Save to file
        val directory = File(context.cacheDir, "pdfs")
        if (!directory.exists()) directory.mkdirs()
        
        val file = File(directory, "W&B_Report_${profile.aircraft.registration}_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
