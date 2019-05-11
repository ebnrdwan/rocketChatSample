package chat.rocket.android.videoconference.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import chat.rocket.android.videoconference.presenter.JitsiVideoConferenceView
import chat.rocket.android.videoconference.presenter.VideoConferencePresenter
import dagger.android.AndroidInjection
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import android.util.Log
import androidx.fragment.app.FragmentActivity
import chat.rocket.android.util.extensions.showToast
import chat.rocket.core.model.MessageType
import com.facebook.react.modules.core.PermissionListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jitsi.meet.sdk.*
import org.greenrobot.eventbus.ThreadMode


fun Context.videoConferenceIntent(chatRoomId: String, chatRoomType: String): Intent =
        Intent(this, VideoConferenceActivity::class.java)
                .putExtra(INTENT_CHAT_ROOM_ID, chatRoomId)
                .putExtra(INTENT_CHAT_ROOM_TYPE, chatRoomType)

private const val INTENT_CHAT_ROOM_ID = "chat_room_id"
private const val INTENT_CHAT_ROOM_TYPE = "chat_room_type"
private const val TAG = "conference_tag"

class VideoConferenceActivity : FragmentActivity(), JitsiMeetActivityInterface, JitsiVideoConferenceView, JitsiMeetViewListener {
    override fun onConferenceJoined(p0: MutableMap<String, Any>?) {

    }

    override fun onConferenceWillJoin(p0: MutableMap<String, Any>?) {
        Log.d("joined", "user joined")
    }

    override fun requestPermissions(permissions: Array<String>, requestCode: Int, listener: PermissionListener) {
        JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    @Inject
    lateinit var presenter: VideoConferencePresenter
    private lateinit var chatRoomId: String
    private lateinit var chatRoomType: String
    private var view: JitsiMeetView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        isCurrentlyInCall = true
        view = JitsiMeetView(this)
        view?.listener = this
        chatRoomId = intent.getStringExtra(INTENT_CHAT_ROOM_ID)
        requireNotNull(chatRoomId) { "no chat_room_id provided in Intent extras" }
        chatRoomType = intent.getStringExtra(INTENT_CHAT_ROOM_TYPE)
        requireNotNull(chatRoomType) { "no chat_room_type provided in Intent extras" }
        presenter.setup(chatRoomId, chatRoomType)
        presenter.initVideoConference()
    }

    override fun onConferenceTerminated(data: Map<String, Any>?) {
        Log.d(TAG, "Conference terminated: " + data!!)

        finishJitsiVideoConference()
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun startJitsiVideoConference(url: String, name: String?) {
        val splits = url.split("t/")
        val server = splits[0].plus("t")
        val room = splits[1]

        val default = JitsiMeetConferenceOptions.Builder()
                .setServerURL(URL(server))
                .setRoom(room)
                .setWelcomePageEnabled(true)
                .build()
        JitsiMeet.setDefaultConferenceOptions(default)
        val options = JitsiMeetConferenceOptions.Builder()
                .setAudioMuted(false)
                .setVideoMuted(true)
                .build()
        view!!.join(options)
        setContentView(view)

    }

    override fun onUserLeaveHint() {
        view?.enterPictureInPicture()
    }

    override fun finishJitsiVideoConference() {
        presenter.sendMessageWithType(chatRoomId, MessageType.endCall())
        finish()
    }

    override fun logJitsiMeetViewState(message: String, map: MutableMap<String, Any>?) =
            Timber.i("$message:  $map")


    override fun onDestroy() {
        super.onDestroy()
        JitsiMeetActivityDelegate.onHostDestroy(this)
        presenter.invalidateTimer()
        isCurrentlyInCall = false
        view!!.dispose()
        view = null

    }

    public override fun onNewIntent(intent: Intent) {
        JitsiMeetActivityDelegate.onNewIntent(intent)
    }


    override fun onResume() {
        super.onResume()
        JitsiMeetActivityDelegate.onHostResume(this)
        isCurrentlyInCall = true
    }

    override fun onStop() {
        super.onStop()
        JitsiMeetActivityDelegate.onHostPause(this)
        isCurrentlyInCall = false
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageType) {
        when (event) {
            is MessageType.endCall -> finish()
        }
    }


    companion object {
        var isCurrentlyInCall = false
    }
}
