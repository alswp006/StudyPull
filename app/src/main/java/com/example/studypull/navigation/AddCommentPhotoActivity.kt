package com.example.studypull.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.example.studypull.R
import com.example.studypull.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.*

class AddCommentPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_comment_photo)

        //Initiate
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        //Open the album
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        //add image upload event
        findViewById<AppCompatButton>(R.id.addphoto_btn_upload).setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK)
            //This is path to selected image
                photoUri = data?.data
            findViewById<ImageView>(R.id.addphoto_image).setImageURI(photoUri)
        } else {
            //Exit the addPhotoActivity if you leave the album without selecting it
            finish()
        }
    }

    fun contentUpload() {
        //make filename

        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "Image_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var commentDTO = ContentDTO.Comment()

            //Insert downloadUri of image
            commentDTO.imageUri = uri.toString()

            //Insert uid of user
            commentDTO.uid = auth?.currentUser?.uid

            //Insert userId
            commentDTO.userId = auth?.currentUser?.email

            //Insert explain of content

            //Insert timestamp
            commentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("comment_images")?.document()?.set(commentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }
    }

    /*/Callback method
    storageRef?.putFile(photoUri!!)?.addOnCanceledListener {
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            //Insert downloadUri of image
            contentDTO.imageUri = uri.toString()

            //Insert uid of user
            contentDTO.uid = auth?.currentUser?.uid

            //Insert userId
            contentDTO.userId = auth?.currentUser?.email

            //Insert explain of content
            contentDTO.explain = findViewById<EditText>(R.id.addphoto_edit_explain).text.toString()

            //Insert timestamp
            contentDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }
    }

}*/


}