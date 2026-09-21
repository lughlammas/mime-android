package com.lughlammas.mime.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

class MapRepository(private val context: Context) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val indexAdapter = moshi.adapter(MapIndex::class.java)
    private val lineAdapter = moshi.adapter(Line::class.java)

    fun listMaps(): List<MapIndexEntry> {
        val json = readAsset("maps/index.json")
        val index = indexAdapter.fromJson(json)
            ?: throw IOException("Failed to parse maps/index.json")
        return index.maps
    }

    fun getMap(path: String): Line {
        val normalized = path.removePrefix("/")
        val json = readAsset(normalized)
        return lineAdapter.fromJson(json)
            ?: throw IOException("Failed to parse $normalized")
    }

    private fun readAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
