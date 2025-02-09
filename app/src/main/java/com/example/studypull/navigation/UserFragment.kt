package com.example.studypull.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.studypull.LoginActivity
import com.example.studypull.MainActivity
import com.example.studypull.R
import com.example.studypull.navigation.model.AlarmDTO
import com.example.studypull.navigation.model.ContentDTO
import com.example.studypull.navigation.model.FollowDTO
import com.example.studypull.navigation.util.FcmPush
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import androidx.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentview : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentview = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            //My page
            fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.text = getString(R.string.signout)
            fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }

        }else{
            //OtherUserPage
            fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow)
            var mainActivity = (activity as MainActivity)
            mainActivity?.findViewById<TextView>(R.id.toolbar_username)?.text = arguments?.getString("userId")
            mainActivity?.findViewById<ImageView>(R.id.toolbar_btn_back)?.setOnClickListener{
                mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.action_home
            }
            mainActivity?.findViewById<ImageView>(R.id.toolbar_title_image)?.visibility = View.GONE
            mainActivity?.findViewById<TextView>(R.id.toolbar_username)?.visibility = View.VISIBLE
            mainActivity?.findViewById<ImageView>(R.id.toolbar_btn_back)?.visibility = View.VISIBLE
            fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.setOnClickListener {
                requestFollow()
            }
        }
        fragmentview?.findViewById<RecyclerView>(R.id.account_recyclerview)?.adapter = UserFragmentRecyclerViewAdapter()
        //임의로 activity -> requireActivity
        fragmentview?.findViewById<RecyclerView>(R.id.account_recyclerview)?.layoutManager = GridLayoutManager(activity,3)

        fragmentview?.findViewById<ImageView>(R.id.account_iv_profile)?.setOnClickListener{
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentview
    }
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentview?.findViewById<TextView>(R.id.account_tv_following_count)?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentview?.findViewById<TextView>(R.id.account_tv_follower_count)?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!)){
                    fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow_cancel)
                    fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow)
                        fragmentview?.findViewById<AppCompatButton>(R.id.account_btn_follow_signout)?.background?.colorFilter = null
                    }
                }
            }

        }
    }
    fun requestFollow(){
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transition ->
            var followDTO = transition.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true

                transition.set(tsDocFollowing,followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followers?.remove(uid)
            }else{
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followers[uid!!] = true
            }
            transition.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transition ->
            var followDTO = transition.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transition.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transition.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid,"StudyPull",message)
    }
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]//밑의 requireActivity는 activity를 수정하였음
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(fragmentview?.findViewById<ImageView>(R.id.account_iv_profile)!!)
            }
        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestore ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener

                //Get data
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO :: class.java)!!)
                }
                fragmentview?.findViewById<TextView>(R.id.account_tv_post_count)?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }


        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(p0.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var imageView = (p0 as CustomViewHolder).imageView
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUri).apply(RequestOptions().centerCrop()).into(imageView)
        }
    }


}