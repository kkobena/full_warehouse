package com.kobe.warehouse.reports.service

import android.content.Context
import android.util.Log
import com.kobe.warehouse.reports.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Service for ML-based sales forecasting using TensorFlow Lite
 * Predicts future sales based on historical data
 */
class ForecastingService private constructor(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val TAG = "ForecastingService"

    // Model parameters
    private val MODEL_INPUT_SIZE = 30  // 30 days of historical data
    private val MODEL_OUTPUT_SIZE = 7  // 7 days forecast
    private val MODEL_VERSION = "1.0.0"

    companion object {
        @Volatile
        private var instance: ForecastingService? = null

        fun getInstance(context: Context): ForecastingService {
            return instance ?: synchronized(this) {
                instance ?: ForecastingService(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize TensorFlow Lite interpreter
     * Load the pre-trained model from assets
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Try to load the TFLite model from assets
            try {
                val modelBuffer = loadModelFile("sales_forecast_model.tflite")
                interpreter = Interpreter(modelBuffer)
                Log.i(TAG, "Forecasting service initialized with TFLite model v$MODEL_VERSION")
            } catch (modelException: Exception) {
                // Model file not found - run in mock mode
                Log.w(TAG, "TFLite model not found, running in mock mode: ${modelException.message}")
                Log.i(TAG, "To use ML forecasting, add 'sales_forecast_model.tflite' to assets folder")
                interpreter = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize forecasting service", e)
            throw e
        }
    }

    /**
     * Load TFLite model file from assets
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Predict future sales based on historical data
     *
     * @param historicalData List of historical daily sales (minimum 30 days)
     * @param forecastDays Number of days to forecast (default 7)
     * @return ForecastData with predictions and recommendations
     */
    suspend fun predictSales(
        historicalData: List<DailySales>,
        forecastDays: Int = 7
    ): ForecastData = withContext(Dispatchers.Default) {

        require(historicalData.size >= MODEL_INPUT_SIZE) {
            "Historical data must contain at least $MODEL_INPUT_SIZE days"
        }

        // Take the most recent data points
        val recentHistory = historicalData.takeLast(MODEL_INPUT_SIZE)

        // Normalize data
        val normalizedData = normalizeData(recentHistory.map { it.amount })

        // Run inference (using mock for now, replace with actual TFLite inference)
        val predictions = if (interpreter != null) {
            runInference(normalizedData, forecastDays)
        } else {
            // Mock predictions with realistic trend
            generateMockPredictions(recentHistory, forecastDays)
        }

        // Denormalize predictions
        val denormalizedPredictions = denormalizeData(predictions, recentHistory.map { it.amount })

        // Generate predicted daily sales
        val startDate = LocalDate.parse(recentHistory.last().date)
        val predictedSales = denormalizedPredictions.mapIndexed { index, amount ->
            DailySales(
                date = startDate.plusDays(index.toLong() + 1).format(DateTimeFormatter.ISO_DATE),
                amount = amount,
                transactionCount = estimateTransactionCount(amount, recentHistory),
                label = "J+${index + 1}"
            )
        }

        // Calculate confidence based on historical variance
        val confidence = calculateConfidence(recentHistory.map { it.amount })

        // Generate recommendations
        val recommendations = generateRecommendations(recentHistory, predictedSales)

        ForecastData(
            historical = recentHistory,
            predicted = predictedSales,
            confidence = confidence,
            recommendations = recommendations
        )
    }

    /**
     * Run TensorFlow Lite inference
     * Uses real model if loaded, otherwise falls back to mock predictions
     */
    private fun runInference(normalizedData: FloatArray, outputSize: Int): FloatArray {
        // Check if we have a real interpreter
        val currentInterpreter = interpreter

        if (currentInterpreter != null) {
            try {
                // Prepare input buffer
                val inputBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE * 4)
                inputBuffer.order(ByteOrder.nativeOrder())
                normalizedData.forEach { inputBuffer.putFloat(it) }
                inputBuffer.rewind()

                // Prepare output buffer
                val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)
                outputBuffer.order(ByteOrder.nativeOrder())

                // Run inference
                currentInterpreter.run(inputBuffer, outputBuffer)

                // Extract results
                outputBuffer.rewind()
                val results = FloatArray(outputSize) { outputBuffer.float }

                Log.d(TAG, "TFLite inference completed successfully")
                return results

            } catch (e: Exception) {
                Log.e(TAG, "TFLite inference failed, falling back to mock", e)
                return generateMockPredictions(normalizedData, outputSize)
            }
        } else {
            // No interpreter loaded - use mock predictions
            Log.d(TAG, "Using mock predictions (no TFLite model loaded)")
            return generateMockPredictions(normalizedData, outputSize)
        }
    }

    /**
     * Generate mock predictions with realistic trends
     * This simulates ML model output for demonstration
     */
    private fun generateMockPredictions(
        historicalData: List<DailySales>,
        days: Int
    ): FloatArray {
        val recentAvg = historicalData.takeLast(7).map { it.amount }.average()
        val trend = calculateTrend(historicalData.map { it.amount })

        return FloatArray(days) { index ->
            val baseValue = recentAvg
            val trendAdjustment = trend * (index + 1) * 0.02
            val seasonality = Math.sin((index + 1) * Math.PI / 7) * 0.05 * baseValue
            (baseValue + trendAdjustment + seasonality).toFloat()
        }
    }

    private fun generateMockPredictions(normalizedData: FloatArray, days: Int): FloatArray {
        val avgValue = normalizedData.average().toFloat()
        val trend = (normalizedData.last() - normalizedData.first()) / normalizedData.size

        return FloatArray(days) { index ->
            avgValue + (trend * (index + 1))
        }
    }

    /**
     * Normalize data to [0, 1] range
     */
    private fun normalizeData(data: List<Double>): FloatArray {
        val max = data.maxOrNull() ?: 1.0
        val min = data.minOrNull() ?: 0.0
        val range = max - min

        return if (range > 0) {
            FloatArray(data.size) { index ->
                ((data[index] - min) / range).toFloat()
            }
        } else {
            FloatArray(data.size) { 0.5f }
        }
    }

    /**
     * Denormalize predictions back to original scale
     */
    private fun denormalizeData(normalized: FloatArray, reference: List<Double>): List<Double> {
        val max = reference.maxOrNull() ?: 1.0
        val min = reference.minOrNull() ?: 0.0
        val range = max - min

        return normalized.map { value ->
            (value * range) + min
        }
    }

    /**
     * Calculate trend from historical data
     * Positive = increasing trend, Negative = decreasing trend
     */
    private fun calculateTrend(data: List<Double>): Double {
        if (data.size < 2) return 0.0

        val firstHalf = data.take(data.size / 2).average()
        val secondHalf = data.takeLast(data.size / 2).average()

        return secondHalf - firstHalf
    }

    /**
     * Calculate confidence level based on historical variance
     * Lower variance = higher confidence
     */
    private fun calculateConfidence(data: List<Double>): Float {
        if (data.size < 2) return 0.5f

        val mean = data.average()
        val variance = data.map { (it - mean) * (it - mean) }.average()
        val stdDev = Math.sqrt(variance)
        val cv = if (mean > 0) stdDev / mean else 1.0 // Coefficient of variation

        // Convert CV to confidence (0.0 to 1.0)
        // Lower CV = higher confidence
        return (1.0 - cv.coerceIn(0.0, 1.0)).toFloat().coerceIn(0.3f, 0.95f)
    }

    /**
     * Estimate transaction count based on average basket
     */
    private fun estimateTransactionCount(amount: Double, historical: List<DailySales>): Int {
        val avgTransactions = historical.filter { it.transactionCount > 0 }
            .map { it.transactionCount }
            .average()

        val avgBasket = historical.filter { it.transactionCount > 0 }
            .map { it.amount / it.transactionCount }
            .average()

        return if (avgBasket > 0) {
            (amount / avgBasket).toInt()
        } else {
            avgTransactions.toInt()
        }
    }

    /**
     * Generate actionable recommendations based on predictions
     */
    private fun generateRecommendations(
        historical: List<DailySales>,
        predicted: List<DailySales>
    ): List<ForecastRecommendation> {
        val recommendations = mutableListOf<ForecastRecommendation>()

        val historicalAvg = historical.map { it.amount }.average()
        val predictedAvg = predicted.map { it.amount }.average()
        val changePercent = ((predictedAvg - historicalAvg) / historicalAvg) * 100

        // Recommendation 1: Sales trend
        when {
            changePercent > 15 -> {
                recommendations.add(
                    ForecastRecommendation(
                        type = RecommendationType.INCREASE_STOCK,
                        title = "Augmenter les stocks",
                        description = "Ventes prévues en hausse de ${String.format("%.1f", changePercent)}%. " +
                                "Assurez-vous d'avoir suffisamment de stock pour les produits populaires.",
                        impact = if (changePercent > 30) RecommendationImpact.HIGH else RecommendationImpact.MEDIUM,
                        expectedChange = changePercent
                    )
                )
            }
            changePercent < -15 -> {
                recommendations.add(
                    ForecastRecommendation(
                        type = RecommendationType.PROMOTION,
                        title = "Créer des promotions",
                        description = "Ventes prévues en baisse de ${String.format("%.1f", abs(changePercent))}%. " +
                                "Envisagez des promotions pour stimuler les ventes.",
                        impact = if (changePercent < -30) RecommendationImpact.HIGH else RecommendationImpact.MEDIUM,
                        expectedChange = changePercent
                    )
                )
            }
        }

        // Recommendation 2: Peak day preparation
        val peakDay = predicted.maxByOrNull { it.amount }
        if (peakDay != null && peakDay.amount > historicalAvg * 1.2) {
            recommendations.add(
                ForecastRecommendation(
                    type = RecommendationType.HIGH_DEMAND,
                    title = "Pic de demande prévu",
                    description = "Forte demande prévue le ${formatDate(peakDay.date)}. " +
                            "Préparez les équipes et les stocks en conséquence.",
                    impact = RecommendationImpact.HIGH,
                    expectedChange = ((peakDay.amount - historicalAvg) / historicalAvg) * 100
                )
            )
        }

        // Recommendation 3: Reorder products
        recommendations.add(
            ForecastRecommendation(
                type = RecommendationType.REORDER_SOON,
                title = "Commander les produits populaires",
                description = "Basé sur les prévisions, planifiez les commandes pour maintenir " +
                        "les niveaux de stock optimaux.",
                impact = RecommendationImpact.MEDIUM,
                expectedChange = null
            )
        )

        return recommendations
    }

    /**
     * Format date for display
     */
    private fun formatDate(isoDate: String): String {
        return try {
            val date = LocalDate.parse(isoDate)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
