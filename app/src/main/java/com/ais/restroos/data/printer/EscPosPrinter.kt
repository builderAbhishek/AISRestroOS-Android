package com.ais.restroos.data.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ais.restroos.data.remote.dtos.ReceiptSettingsDto
import com.ais.restroos.domain.model.ClosedOrder
import com.ais.restroos.domain.model.HeldOrder
import com.ais.restroos.domain.model.OrderItem
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object EscPosPrinter {

    private val INIT = byteArrayOf(0x1B, 0x40)
    private val FEED_LINES = byteArrayOf(0x1B, 0x64, 0x03)
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

    private val priceFormat = DecimalFormat("#,##0.00")

    @Volatile
    var activeReceiptSettings: ReceiptSettingsDto? = null

    @Volatile
    var activeQrBitmap: Bitmap? = null

    fun updateActiveReceiptConfig(settings: ReceiptSettingsDto, qrBitmap: Bitmap?) {
        activeReceiptSettings = settings
        activeQrBitmap = qrBitmap
    }

    private fun fmtAmount(value: Double): String = priceFormat.format(value)

    fun buildTestReceipt(paperWidth: String = "58mm"): ByteArray {
        val effectiveWidth = activeReceiptSettings?.paper_width?.ifBlank { paperWidth } ?: paperWidth
        val fontSize = activeReceiptSettings?.font_size ?: 18
        val builder = ReceiptCanvasBuilder(effectiveWidth, fontSize)
        builder.addCenteredText("AISRestroOS POS", sizeOffset = 4f, bold = true)
        builder.addCenteredText("Thermal Printer Test", sizeOffset = 0f, bold = false)
        builder.addDivider()
        builder.addLeftText("Status: Connection Successful")
        builder.addLeftText("Paper Width: $effectiveWidth (Font: ${fontSize}px)")
        builder.addLeftText("Hindi Test: पनीर टिक्का (Half)")
        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date())
        builder.addLeftText("Date: $dateStr")
        builder.addDivider()
        builder.addCenteredText("Thank you for using AISRestroOS!", sizeOffset = -1f)

        return finalizeReceipt(builder.buildBitmap(), effectiveWidth)
    }

    /**
     * Renders a complete bill bitmap (58mm = 384px, 80mm = 576px) used for both
     * live on-screen preview and thermal printer output.
     */
    fun buildOrderBillPreviewBitmap(
        order: ClosedOrder,
        restaurantName: String = "RESTAURANT",
        settings: ReceiptSettingsDto? = activeReceiptSettings,
        qrBitmap: Bitmap? = activeQrBitmap
    ): Bitmap {
        val paperWidth = settings?.paper_width?.ifBlank { "58mm" } ?: "58mm"
        val baseFontSize = settings?.font_size ?: 18
        val builder = ReceiptCanvasBuilder(paperWidth, baseFontSize)

        // 1. Custom Header (first line bold title, remaining lines subtitle/address/GSTIN)
        val rawHeader = settings?.header_text?.trim().orEmpty().ifBlank { restaurantName.ifBlank { "RESTAURANT" } }
        val headerLines = rawHeader.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        headerLines.forEachIndexed { index, line ->
            if (index == 0) {
                builder.addCenteredText(line, sizeOffset = 3f, bold = true)
            } else {
                builder.addCenteredText(line, sizeOffset = -1.5f, bold = false)
            }
        }
        builder.addCenteredText("TAX INVOICE / BILL", sizeOffset = -0.5f, bold = false)
        builder.addDivider()

        // 2. Invoice / Table / Date
        builder.addLeftText("Invoice No: ${order.invoiceNumber}")
        val tableLabel = if (!order.tableNumber.isNullOrBlank()) "Table No: ${order.tableNumber}" else "Table: Takeaway"
        builder.addLeftText(tableLabel)
        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(order.completedTimestamp))
        builder.addLeftText("Date: $dateStr")
        builder.addDivider()

        // 3. Item Table Header: Item | Qty | Price | Amount
        builder.addItemTableHeader()
        builder.addDivider()

        // 4. Items (supports Hindi/Unicode + multi-line wrapping + Qty, Unit Price, Line Amount)
        order.items.forEach { item ->
            val unitPrice = item.price
            val lineTotal = unitPrice * item.quantity
            builder.addBillItemRow(
                name = item.name,
                qty = item.quantity,
                unitPrice = fmtAmount(unitPrice),
                lineTotal = fmtAmount(lineTotal)
            )
        }

        builder.addDivider()

        // 5. Totals, Discount & Service Charge (No INR)
        val computedSubtotal = if (order.subtotal > 0.0) {
            order.subtotal
        } else {
            order.items.sumOf { it.price * it.quantity }
        }
        val hasExtraCharges = order.discountAmount > 0.0 || order.serviceCharge > 0.0 || order.taxAmount > 0.0
        if (hasExtraCharges) {
            builder.addKeyValueRow("Subtotal", fmtAmount(computedSubtotal))
        }
        if (order.discountAmount > 0.0) {
            builder.addKeyValueRow("Discount", "-${fmtAmount(order.discountAmount)}")
        }
        if (order.serviceCharge > 0.0) {
            builder.addKeyValueRow("Service Charge", "+${fmtAmount(order.serviceCharge)}")
        }
        if (order.taxAmount > 0.0) {
            builder.addKeyValueRow("Tax", "+${fmtAmount(order.taxAmount)}")
        }
        if (hasExtraCharges) {
            builder.addDivider()
        }

        builder.addKeyValueRow(
            label = "GRAND TOTAL",
            value = "₹${fmtAmount(order.totalAmount)}",
            bold = true,
            sizeOffset = 3f
        )
        builder.addDivider()

        // 6. Payment QR Code (if enabled & available)
        if ((settings?.show_qr != false) && qrBitmap != null) {
            builder.addCenteredText("Scan & Pay", sizeOffset = -1f, bold = false)
            builder.addCenteredBitmap(qrBitmap)
            builder.addDivider()
        }

        // 7. Custom Footer + Fixed Branding
        val rawFooter = settings?.footer_text?.trim().orEmpty().ifBlank { "Thank you! Visit Again" }
        val footerLines = rawFooter.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.contains("AISRestroOS", ignoreCase = true) }
        footerLines.forEachIndexed { idx, line ->
            builder.addCenteredText(
                text = line,
                sizeOffset = if (idx == 0) -0.5f else -1.5f,
                bold = false
            )
        }
        builder.addCenteredText("Powered by AISRestroOS", sizeOffset = -2f, bold = false)

        return builder.buildBitmap()
    }

    fun buildOrderBillReceipt(
        order: ClosedOrder,
        restaurantName: String = "RESTAURANT",
        paperWidth: String = "58mm"
    ): ByteArray {
        val baseSettings = activeReceiptSettings ?: ReceiptSettingsDto(
            header_text = restaurantName,
            paper_width = paperWidth
        )
        val effectiveSettings = if (baseSettings.paper_width.isBlank()) {
            baseSettings.copy(paper_width = paperWidth)
        } else {
            baseSettings
        }
        val bitmap = buildOrderBillPreviewBitmap(
            order = order,
            restaurantName = restaurantName,
            settings = effectiveSettings,
            qrBitmap = activeQrBitmap
        )
        return finalizeReceipt(bitmap, effectiveSettings.paper_width)
    }

    fun buildKotReceipt(
        order: HeldOrder,
        itemsToPrint: List<OrderItem> = order.items,
        isAddOn: Boolean = false,
        restaurantName: String = "",
        paperWidth: String = "58mm"
    ): ByteArray {
        val effectiveWidth = activeReceiptSettings?.paper_width?.ifBlank { paperWidth } ?: paperWidth
        val fontSize = activeReceiptSettings?.font_size ?: 18
        val builder = ReceiptCanvasBuilder(effectiveWidth, fontSize)

        builder.addCenteredText(
            if (isAddOn) "KOT (ADD-ON / RUNNING)" else "KITCHEN ORDER TICKET",
            sizeOffset = 2f,
            bold = true
        )
        builder.addDivider()

        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        builder.addKeyValueRow("Date: $dateStr", "Time: $timeStr")

        val kotDisplay = order.kotNumber?.ifBlank { null }
            ?: if (order.orderId.length > 12) order.orderId.take(8).uppercase() else order.orderId
        val kotPart = if (isAddOn) "KOT: $kotDisplay (+)" else "KOT: $kotDisplay"
        val tablePart = if (!order.tableNumber.isNullOrBlank()) "Table: ${order.tableNumber}" else "Table: Takeaway"
        builder.addKeyValueRow(kotPart, tablePart, bold = true)

        val serviceType = when (order.orderType.uppercase()) {
            "TAKEAWAY" -> "Takeaway"
            "DELIVERY" -> "Delivery"
            else -> "Dine In"
        }
        builder.addLeftText("Service: $serviceType")
        builder.addDivider()

        builder.addKeyValueRow("ITEM", "QTY", bold = true)
        builder.addDivider()

        itemsToPrint.forEach { item ->
            val qtyDisplay = if (isAddOn) "+${item.quantity}" else "${item.quantity}"
            builder.addKotItemRow(item.name, qtyDisplay)
        }

        builder.addDivider()
        if (isAddOn) {
            builder.addCenteredText("* NEW ITEMS ONLY *", bold = true)
        }
        builder.addCenteredText("KITCHEN COPY", sizeOffset = -1f, bold = true)

        return finalizeReceipt(builder.buildBitmap(), effectiveWidth)
    }

    private fun finalizeReceipt(bitmap: Bitmap, paperWidth: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write(INIT)
        stream.write(bitmapToEscPosRaster(bitmap))
        stream.write(FEED_LINES)
        if (paperWidth == "80mm") {
            stream.write(CUT_PAPER)
        }
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * Converts an Android Bitmap into chunked ESC/POS GS v 0 raster graphics commands.
     */
    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val out = ByteArrayOutputStream()
        val maxSliceHeight = 200
        var startY = 0

        while (startY < height) {
            val sliceHeight = min(maxSliceHeight, height - startY)
            // GS v 0 m xL xH yL yH
            out.write(0x1D)
            out.write(0x76)
            out.write(0x30)
            out.write(0x00)
            out.write(bytesPerRow and 0xFF)
            out.write((bytesPerRow shr 8) and 0xFF)
            out.write(sliceHeight and 0xFF)
            out.write((sliceHeight shr 8) and 0xFF)

            for (y in startY until (startY + sliceHeight)) {
                val rowOffset = y * width
                for (byteIdx in 0 until bytesPerRow) {
                    var b = 0
                    val baseX = byteIdx * 8
                    for (bit in 0 until 8) {
                        val x = baseX + bit
                        if (x < width) {
                            val color = pixels[rowOffset + x]
                            val r = (color shr 16) and 0xFF
                            val g = (color shr 8) and 0xFF
                            val bl = color and 0xFF
                            val lum = (r * 299 + g * 587 + bl * 114) / 1000
                            if (lum < 128) {
                                b = b or (1 shl (7 - bit))
                            }
                        }
                    }
                    out.write(b)
                }
            }
            startY += sliceHeight
        }

        return out.toByteArray()
    }

    private class ReceiptCanvasBuilder(paperWidth: String, baseFontSizePx: Int = 18) {
        companion object {
            private val FIXED_REGULAR: Typeface by lazy {
                loadSystemFont("/system/fonts/Roboto-Regular.ttf")
                    ?: Typeface.create("sans-serif", Typeface.NORMAL)
            }
            private val FIXED_BOLD: Typeface by lazy {
                loadSystemFont("/system/fonts/Roboto-Bold.ttf")
                    ?: loadSystemFont("/system/fonts/Roboto-Medium.ttf")
                    ?: Typeface.create("sans-serif", Typeface.BOLD)
            }

            private fun loadSystemFont(path: String): Typeface? {
                return try {
                    val f = java.io.File(path)
                    if (f.exists()) Typeface.createFromFile(f) else null
                } catch (_: Exception) {
                    null
                }
            }
        }

        private val widthPx = if (paperWidth == "80mm") 576 else 384
        private val scale = if (paperWidth == "80mm") 1.35f else 1.0f
        private val paddingX = (6f * scale).toInt()
        private val contentWidth = widthPx - (paddingX * 2)
        private val baseSize = baseFontSizePx.coerceIn(14, 24).toFloat() * scale
        private val lineGap = (5f * scale)

        private val drawOps = mutableListOf<(Canvas, Float) -> Float>()
        private var totalHeight = (8f * scale)

        private fun makePaint(sizeOffset: Float = 0f, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT): TextPaint {
            return TextPaint(0).apply {
                isAntiAlias = false
                isSubpixelText = false
                color = Color.BLACK
                textSize = max(11f, baseSize + (sizeOffset * scale))
                typeface = if (bold) FIXED_BOLD else FIXED_REGULAR
                textAlign = align
            }
        }

        fun addCenteredText(text: String, sizeOffset: Float = 0f, bold: Boolean = false) {
            val paint = makePaint(sizeOffset, bold)
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.05f)
                .build()
            val h = layout.height.toFloat() + lineGap
            drawOps.add { canvas, y ->
                canvas.save()
                canvas.translate(paddingX.toFloat(), y)
                layout.draw(canvas)
                canvas.restore()
                y + h
            }
            totalHeight += h
        }

        fun addLeftText(text: String, sizeOffset: Float = 0f, bold: Boolean = false) {
            val paint = makePaint(sizeOffset, bold)
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.05f)
                .build()
            val h = layout.height.toFloat() + lineGap
            drawOps.add { canvas, y ->
                canvas.save()
                canvas.translate(paddingX.toFloat(), y)
                layout.draw(canvas)
                canvas.restore()
                y + h
            }
            totalHeight += h
        }

        fun addCenteredBitmap(src: Bitmap) {
            val targetSize = (240f * scale).toInt()
            val left = (widthPx - targetSize) / 2
            val bmpPaint = Paint(0).apply {
                isFilterBitmap = false
                isDither = false
            }
            drawOps.add { canvas, y ->
                val dest = Rect(left, y.toInt(), left + targetSize, y.toInt() + targetSize)
                canvas.drawBitmap(src, null, dest, bmpPaint)
                y + targetSize + lineGap
            }
            totalHeight += targetSize + lineGap
        }

        fun addDivider() {
            val h = (10f * scale)
            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = max(1.5f, 1.5f * scale)
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * scale, 4f * scale), 0f)
            }
            drawOps.add { canvas, y ->
                val lineY = y + (h / 2f)
                canvas.drawLine(paddingX.toFloat(), lineY, (widthPx - paddingX).toFloat(), lineY, linePaint)
                y + h
            }
            totalHeight += h
        }

        fun addItemTableHeader() {
            val paintLeft = makePaint(sizeOffset = -1.5f, bold = false, align = Paint.Align.LEFT)
            val paintRight = makePaint(sizeOffset = -1.5f, bold = false, align = Paint.Align.RIGHT)
            val fm = paintLeft.fontMetrics
            val rowH = (fm.descent - fm.ascent) + lineGap

            val qtyRightX = paddingX + (contentWidth * 0.42f)
            val rateRightX = paddingX + (contentWidth * 0.70f)
            val amtRightX = (widthPx - paddingX).toFloat()

            drawOps.add { canvas, y ->
                val baseline = y - fm.ascent
                canvas.drawText("Item", paddingX.toFloat(), baseline, paintLeft)
                canvas.drawText("Qty", qtyRightX, baseline, paintRight)
                canvas.drawText("Price", rateRightX, baseline, paintRight)
                canvas.drawText("Amount", amtRightX, baseline, paintRight)
                y + rowH
            }
            totalHeight += rowH
        }

        fun addBillItemRow(name: String, qty: Int, unitPrice: String, lineTotal: String) {
            val namePaint = makePaint(sizeOffset = -0.5f, bold = false)
            val numPaint = makePaint(sizeOffset = -1f, bold = false, align = Paint.Align.RIGHT)
            val amtPaint = makePaint(sizeOffset = -1f, bold = false, align = Paint.Align.RIGHT)

            val nameLayout = StaticLayout.Builder.obtain(name, 0, name.length, namePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.05f)
                .build()

            val fm = numPaint.fontMetrics
            val numbersH = (fm.descent - fm.ascent) + (lineGap * 1.1f)
            val blockH = nameLayout.height.toFloat() + (2f * scale) + numbersH

            val qtyRightX = paddingX + (contentWidth * 0.42f)
            val rateRightX = paddingX + (contentWidth * 0.70f)
            val amtRightX = (widthPx - paddingX).toFloat()

            drawOps.add { canvas, y ->
                canvas.save()
                canvas.translate(paddingX.toFloat(), y)
                nameLayout.draw(canvas)
                canvas.restore()

                val numTop = y + nameLayout.height.toFloat() + (2f * scale)
                val baseline = numTop - fm.ascent
                canvas.drawText("x$qty", qtyRightX, baseline, numPaint)
                canvas.drawText(unitPrice, rateRightX, baseline, numPaint)
                canvas.drawText(lineTotal, amtRightX, baseline, amtPaint)

                y + blockH
            }
            totalHeight += blockH
        }

        fun addKotItemRow(name: String, qtyStr: String) {
            val qtyColW = (56f * scale).toInt()
            val nameColW = contentWidth - qtyColW - (8f * scale).toInt()

            val namePaint = makePaint(sizeOffset = 0f, bold = false)
            val qtyPaint = makePaint(sizeOffset = 0.5f, bold = true, align = Paint.Align.RIGHT)

            val nameLayout = StaticLayout.Builder.obtain(name, 0, name.length, namePaint, nameColW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.05f)
                .build()

            val fm = qtyPaint.fontMetrics
            val qtyH = fm.descent - fm.ascent
            val rowH = max(nameLayout.height.toFloat(), qtyH) + (lineGap * 1.2f)

            drawOps.add { canvas, y ->
                canvas.save()
                canvas.translate(paddingX.toFloat(), y)
                nameLayout.draw(canvas)
                canvas.restore()

                canvas.drawText(qtyStr, (widthPx - paddingX).toFloat(), y - fm.ascent, qtyPaint)
                y + rowH
            }
            totalHeight += rowH
        }

        fun addKeyValueRow(label: String, value: String, bold: Boolean = false, sizeOffset: Float = 0f) {
            val leftPaint = makePaint(sizeOffset, bold, Paint.Align.LEFT)
            val rightPaint = makePaint(sizeOffset, bold, Paint.Align.RIGHT)
            val fm = leftPaint.fontMetrics
            val rowH = (fm.descent - fm.ascent) + lineGap

            drawOps.add { canvas, y ->
                val baseline = y - fm.ascent
                canvas.drawText(label, paddingX.toFloat(), baseline, leftPaint)
                canvas.drawText(value, (widthPx - paddingX).toFloat(), baseline, rightPaint)
                y + rowH
            }
            totalHeight += rowH
        }

        fun buildBitmap(): Bitmap {
            val bmpHeight = max(32, ceil(totalHeight + (10f * scale)).toInt())
            val bitmap = Bitmap.createBitmap(widthPx, bmpHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            var currentY = 6f * scale
            drawOps.forEach { op ->
                currentY = op(canvas, currentY)
            }
            return bitmap
        }
    }
}
