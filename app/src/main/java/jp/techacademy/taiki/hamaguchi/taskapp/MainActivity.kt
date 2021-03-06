package jp.techacademy.taiki.hamaguchi.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*


const val EXTRA_TASK = "jp.techacademy.taro.kirameki.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            if(serch_edit_text.text.toString() == "") {
                reloadListView(null)
            } else {
                reloadListView(serch_edit_text.text.toString())
            }
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        serch_edit_text.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        serch_edit_text.setOnEditorActionListener(object :TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                if (p1 == EditorInfo.IME_ACTION_DONE) {
                    // ??????(DONE)??????????????????????????????
                    if(serch_edit_text.text.toString() == "") {
                        reloadListView(null)
                    } else {
                        reloadListView(serch_edit_text.text.toString())
                    }
                }
                return true
            }

        })

        // Realm?????????
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListView?????????
        mTaskAdapter = TaskAdapter(this)

        // ListView?????????????????????????????????
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // ?????????????????????????????????????????????
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListView?????????????????????????????????
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // ????????????????????????
            val task = parent.adapter.getItem(position) as Task

            // ??????????????????????????????
            val builder = AlertDialog.Builder(this)

            builder.setTitle("??????")
            builder.setMessage(task.title + "?????????????????????")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                if(serch_edit_text.text.toString() == "") {
                    reloadListView(null)
                } else {
                    reloadListView(serch_edit_text.text.toString())
                }
            }

            /* ??????????????????????????????
            builder.setPositiveButton("a", object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                    mRealm.beginTransaction()
                    results.deleteAllFromRealm()
                    mRealm.commitTransaction()

                    reloadListView()
                }

            })
             */

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView(null)
    }

    private fun reloadListView(text: String?) {
        // Realm??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        var taskRealmResults: RealmResults<Task>
        if (text == null) {
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        } else {
            taskRealmResults = mRealm.where(Task::class.java).equalTo("category", text).findAll().sort("date", Sort.DESCENDING)
        }

        // ?????????????????????TaskList????????????????????????
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // Task???ListView???????????????????????????
        listView1.adapter = mTaskAdapter

        // ???????????????????????????????????????????????????????????????????????????????????????????????????
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val focusView = currentFocus ?: return false
        focusView.requestFocus()
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

}