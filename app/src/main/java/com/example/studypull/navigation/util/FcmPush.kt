package com.example.studypull.navigation.util

import android.content.ContentValues.TAG
import android.util.Log
import com.example.studypull.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.okhttp.*
import java.io.IOException

class FcmPush {

    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAA0thAPIY:APA91bHfn181bhtLpOmfAbbuoyJDvxe6hMyK7Szn52UvWm0qLdcM3XKsM9B5IpIybQhEn5eKV-2m-tq39TsVnl5PMd_WY2iHgs1fmGkP2rLTYTIxCtyTZWiFMzjmDYl3_bwpkFx9CSZi"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null
    companion object{
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(destinationUid : String, title : String, message : String) {
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization","key="+serverKey)
                    .url(url)
                    .post(body)
                    .build()
                Log.d("pushalarm","sendMessage")

                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    override fun onFailure(request: Request?, e: IOException?) {
                        TODO("Not yet implemented")
                    }

                    override fun onResponse(response: Response?) {
                        println(response?.body()?.string())
                        Log.d("pushalarm","onResponse")
                    }

                })
            }
        }
    }
}