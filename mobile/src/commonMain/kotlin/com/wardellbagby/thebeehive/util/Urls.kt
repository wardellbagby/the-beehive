package com.wardellbagby.thebeehive.util

private val URL_REGEX =
  Regex(
    "https?://" +
      "(" +
      // An IPv4 address
      "((\\d{1,3}\\.){3}\\d{1,3})" +
      // or...
      "|" +
      // a hostname and tld
      "([a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*)" +
      ")" +
      // Optional port
      "(:\\d{1,5})?" +
      // Optional path, query, and fragment
      "(/\\S*)?"
  )

fun CharSequence.isValidUrl(): Boolean = URL_REGEX.matchEntire(this) != null
