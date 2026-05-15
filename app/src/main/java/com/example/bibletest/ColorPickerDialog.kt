package com.example.bibletest

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.*

class ColorPickerDialog(
    context: Context,
    initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) {

    private val dialog: AlertDialog

    private var hsv = FloatArray(3)
    private var currentColor = initialColor

    init {
        Color.colorToHSV(initialColor, hsv)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // ── PREVIEW ─────────────────────────────
        val preview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80
            ).apply { bottomMargin = 20 }
            setBackgroundColor(initialColor)
        }
        root.addView(preview)

        // ── COLOR WHEEL ─────────────────────────
        val wheelSize = 500

        val wheelView = object : View(context) {

            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val radius = wheelSize / 2f

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                val bitmap = Bitmap.createBitmap(wheelSize, wheelSize, Bitmap.Config.ARGB_8888)

                for (y in 0 until wheelSize) {
                    for (x in 0 until wheelSize) {
                        val dx: Float = x - radius
                        val dy: Float = y - radius
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                        if (dist <= radius) {
                            val hue = ((kotlin.math.atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                            val sat = (dist / radius).toFloat()

                            val rgb = Color.HSVToColor(floatArrayOf(
                                hue.toFloat(),
                                sat,
                                hsv[2]
                            ))

                            bitmap.setPixel(x, y, rgb)
                        }
                    }
                }

                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                // selector
                val angle = Math.toRadians(hsv[0].toDouble()).toFloat()
                val sx = radius + kotlin.math.cos(angle) * hsv[1] * radius
                val sy = radius + kotlin.math.sin(angle) * hsv[1] * radius

                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 5f
                canvas.drawCircle(sx.toFloat(), sy.toFloat(), 10f, paint)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val dx = event.x - wheelSize / 2f
                val dy = event.y - wheelSize / 2f

                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                hsv[0] = ((Math.atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI) + 360).toFloat() % 360
                hsv[1] = (dist / (wheelSize / 2f)).coerceIn(0f, 1f)

                update()
                return true
            }

            private fun update() {
                currentColor = Color.HSVToColor(hsv)
                preview.setBackgroundColor(currentColor)
                invalidate()
            }
        }

        root.addView(wheelView, LinearLayout.LayoutParams(wheelSize, wheelSize))

        // ── BRIGHTNESS SLIDER ───────────────────
        val slider = SeekBar(context).apply {
            max = 100
            progress = (hsv[2] * 100).toInt()

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    hsv[2] = progress / 100f
                    currentColor = Color.HSVToColor(hsv)
                    preview.setBackgroundColor(currentColor)
                    wheelView.invalidate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        root.addView(slider)

        // ── HEX INPUT ───────────────────────────
        val hexInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(String.format("#%06X", 0xFFFFFF and initialColor))

            setOnEditorActionListener { _, _, _ ->
                try {
                    val color = Color.parseColor(text.toString())
                    Color.colorToHSV(color, hsv)
                    currentColor = color
                    preview.setBackgroundColor(color)
                    wheelView.invalidate()
                } catch (_: Exception) {}
                false
            }
        }

        root.addView(hexInput)

        // ── DIALOG ─────────────────────────────
        dialog = AlertDialog.Builder(context)
            .setTitle("Pick Color")
            .setView(root)
            .setPositiveButton("OK") { _, _ ->
                onColorSelected(currentColor)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    fun show() {
        dialog.show()
    }
}