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
<pre><code>
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
    </code></pre>

### 후에 코루틴 방식으로 변경
https://developer.android.com/kotlin/coroutines?hl=ko.  
https://firebase.google.com/docs/reference/android/com/google/firebase/storage/UploadTask.TaskSnapshot

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
    </code></pre>.   
### Glide 를 이용하여 이미지를 받아와서 피드에 표현
<pre><code>
override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholder = (p0 as CustomViewHolder).itemView

            //UserId
            viewholder.findViewById<TextView>(R.id.detailviewitem_profile_textview).text =
                contentDTOs!![p1].userId

            //Image
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUri)
                .into(viewholder.findViewById<ImageView>(R.id.detailviewitem_imageview_content))

            //Explain of context
            viewholder.findViewById<TextView>(R.id.detailviewitem_explain_textview).text =
                contentDTOs!![p1].explain

            //likes
            viewholder.findViewById<TextView>(R.id.detailviewitem_favoritecounter_textview).text =
                "Likes " + contentDTOs!![p1].favoriteCount

            //ProfileImage
            firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                if(documentSnapshot.data != null) {

                    var url = documentSnapshot?.data!!["image"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(viewholder.findViewById<ImageView>(R.id.detailviewitem_profile_image))
                }
            }

            //This code is when button is clicked
            viewholder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview)
                .setOnClickListener {
                    favoriteEvent(p1)
                }

            //This code is when the page is loaded
            if (contentDTOs[p1].favorites.containsKey(uid)) {
                //This is like status
                viewholder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview)
                    .setImageResource(R.drawable.ic_favorite)
            } else {
                //This is unlike status
                viewholder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview)
                    .setImageResource(R.drawable.ic_favorite_border)
            }

            viewholder.findViewById<ImageView>(R.id.detailviewitem_profile_image).setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[p1].uid)
                bundle.putString("destinationUid",contentDTOs[p1].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            viewholder.findViewById<ImageView>(R.id.detailviewitem_comment_imageview).setOnClickListener { v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[p1])
                intent.putExtra("destinationUid",contentDTOs[p1].uid)
                startActivity(intent)
            }

        }
        </code></pre>
