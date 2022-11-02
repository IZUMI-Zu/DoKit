package com.didichuxing.doraemondemo.designcheck

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.blankj.utilcode.util.ToastUtils
import com.didichuxing.doraemondemo.R
import com.didichuxing.doraemondemo.test.ScreenRecordingTest
import com.didichuxing.doraemonkit.DoKit
import com.didichuxing.doraemonkit.constant.BundleKey
import com.didichuxing.doraemonkit.kit.fileexplorer.ImageDetailFragment
import com.didichuxing.doraemonkit.kit.test.report.ScreenShotManager
import java.io.File

/**
 * Design Check Demo Activity
 */
class DCActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MCActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dc)
    }

}
