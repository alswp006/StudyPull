# StudyPull
SNS for stududent
## 함께 공부하고 싶은 사람들을 위한 앱 입니다.
기본적인 기능으로는 자신이 궁금한 문제나 공부진행상황을 실시간으로 피드에 올려 즉각적인 피드백을 받을수 있게하고, 그 외에도 유저간 대화기능 등을 지원예정중에 있는 앱입니다.
## 기본적인 앱 구상 회의
![meeting1](https://user-images.githubusercontent.com/99385873/178091513-ae76d01f-c75d-4e05-942c-8b907a3003d8.png)
## 사용한 기술스택
### firebaseAuth를 이용한 구글 연동 이메일 로그인 기능 구현
https://firebase.google.com/docs/auth/android/google-signin?hl=ko
### recyclerView를 사용하여 피드기능 구성
https://developer.android.com/guide/topics/ui/layout/recyclerview?hl=ko
### promise 방식을 이용한 비동기 프로그래밍을 이용해 이미지 파이어베이스에 업로드
https://stackoverflow.com/questions/61610024/how-to-upload-an-image-to-firebase-storage-using-kotlin-in-android-q 참고
### 후에 코루틴 방식으로 변경
https://developer.android.com/kotlin/coroutines?hl=ko

<pre><code>	
fun contentUpload(): Task<Uri> {

        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "Image_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        GlobalScope.launch(Dispatchers.IO){
            storageRef?.putFile(photoUri!!)?.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                //Insert downloadUri of image
                contentDTO.imageUri = uri.toString()

                //Insert uid of user
                contentDTO.uid = auth?.currentUser?.uid

                //Insert userId
                contentDTO.userId = auth?.currentUser?.email

                //Insert explain of content
                contentDTO.explain =
                    findViewById<EditText>(R.id.addphoto_edit_explain).text.toString()

                //Insert timestamp
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }?.await()
        }
        return storageRef!!.downloadUrl
    }
    </code></pre>
