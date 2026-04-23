package com.wardellbagby.thebeehive.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.Padding

val shortDateTimeFormat = LocalDateTime.Format {
    monthNumber(Padding.NONE)
    chars("/")
    day(Padding.NONE)
    chars("/")
    yearTwoDigits(1990)
    chars(" ")
    amPmHour(Padding.NONE)
    chars(":")
    minute()
    chars(" ")
    amPmMarker("am", "pm")
}