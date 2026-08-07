/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.utils

object AndroidApiLevels {

    private val API_LEVEL_MAP: Map<Int, Triple<String, String, String>> = mapOf(
        1 to Triple("Android 1.0", "BASE", ""),
        2 to Triple("Android 1.1", "BASE_1_1", "Petit Four"),
        3 to Triple("Android 1.5", "CUPCAKE", "Cupcake"),
        4 to Triple("Android 1.6", "DONUT", "Donut"),
        5 to Triple("Android 2.0", "ECLAIR", "Eclair"),
        6 to Triple("Android 2.0.1", "ECLAIR_0_1", "Eclair"),
        7 to Triple("Android 2.1", "ECLAIR_MR1", "Eclair"),
        8 to Triple("Android 2.2", "FROYO", "Froyo"),
        9 to Triple("Android 2.3.0 - 2.3.2", "GINGERBREAD", "Gingerbread"),
        10 to Triple("Android 2.3.3 - 2.3.7", "GINGERBREAD_MR1", "Gingerbread"),
        11 to Triple("Android 3.0", "HONEYCOMB", "Honeycomb"),
        12 to Triple("Android 3.1", "HONEYCOMB_MR1", "Honeycomb"),
        13 to Triple("Android 3.2", "HONEYCOMB_MR2", "Honeycomb"),
        14 to Triple("Android 4.0.1 - 4.0.2", "ICE_CREAM_SANDWICH", "Ice Cream Sandwich"),
        15 to Triple("Android 4.0.3 - 4.0.4", "ICE_CREAM_SANDWICH_MR1", "Ice Cream Sandwich"),
        16 to Triple("Android 4.1", "JELLY_BEAN", "Jelly Bean"),
        17 to Triple("Android 4.2", "JELLY_BEAN_MR1", "Jelly Bean"),
        18 to Triple("Android 4.3", "JELLY_BEAN_MR2", "Jelly Bean"),
        19 to Triple("Android 4.4", "KITKAT", "KitKat"),
        20 to Triple("Android 4.4W", "KITKAT_WATCH", "KitKat"),
        21 to Triple("Android 5.0", "L", "Lollipop"),
        22 to Triple("Android 5.1", "L_MR1", "Lollipop"),
        23 to Triple("Android 6", "M", "Marshmallow"),
        24 to Triple("Android 7.0", "N", "Nougat"),
        25 to Triple("Android 7.1", "N_MR1", "Nougat"),
        26 to Triple("Android 8.0", "O", "Oreo"),
        27 to Triple("Android 8.1", "O_MR1", "Oreo"),
        28 to Triple("Android 9", "P", "Pie"),
        29 to Triple("Android 10", "Q", "Quince Tart"),
        30 to Triple("Android 11", "R", "Red Velvet Cake"),
        31 to Triple("Android 12", "S", "Snow Cone"),
        32 to Triple("Android 12L", "S_V2", "Snow Cone"),
        33 to Triple("Android 13", "T", "Tiramisu"),
        34 to Triple("Android 14", "U", "Upside Down Cake"),
        35 to Triple("Android 15", "V", "Vanilla Ice Cream"),
        36 to Triple("Android 16", "B", "Baklava"),
        37 to Triple("Android 17", "C", "Cinnamon Bun"),
    )

    fun getAndroidVersionForApiLevel(apiLevel: Int): String = API_LEVEL_MAP[apiLevel]?.first ?: "Unknown"
    fun getAndroidVersionCodeForApiLevel(apiLevel: Int): String = API_LEVEL_MAP[apiLevel]?.second ?: "Unknown"
    fun getAndroidCodenameForApiLevel(apiLevel: Int): String = API_LEVEL_MAP[apiLevel]?.third ?: "Unknown"
}
