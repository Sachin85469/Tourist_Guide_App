package com.arriva.touristguideapp.data.sos

import java.io.Serializable

data class SOSContact(
    var id: String = "",
    var name: String = "",
    var phone: String = ""
) : Serializable
