package afkt.app.ui.fragment

import afkt.app.R
import afkt.app.base.BaseFragment
import afkt.app.databinding.FragmentAppBinding
import afkt.app.module.ActionEnum
import afkt.app.module.FileApkItem
import afkt.app.module.TypeEnum
import afkt.app.ui.adapter.ApkListAdapter
import afkt.app.utils.AppSearchUtils
import afkt.app.utils.ScanSDCardUtils
import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.tt.whorlviewlibrary.WhorlView
import dev.callback.common.DevCallback
import dev.utils.DevFinal
import dev.utils.app.ListViewUtils
import dev.utils.app.ResourceUtils
import dev.utils.app.TextViewUtils
import dev.utils.app.ViewUtils
import dev.utils.app.logger.DevLogger
import dev.utils.app.permission.PermissionUtils
import dev.utils.app.toast.ToastTintUtils
import dev.utils.common.FileUtils
import dev.utils.common.HtmlUtils
import dev.widget.assist.ViewAssist
import dev.widget.function.StateLayout
import java.util.*

class ScanSDCardFragment : BaseFragment<FragmentAppBinding>() {

    // = View =

    private var whorlView: WhorlView? = null

    // = Object =

    private var type = TypeEnum.QUERY_APK
    private var searchContent: String = ""

    override fun baseContentId(): Int {
        return R.layout.fragment_app
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        ScanSDCardUtils.instance.setCallback(object : DevCallback<ArrayList<FileApkItem>>() {
            override fun callback(value: ArrayList<FileApkItem>?) {
                viewModel.sdcardScan.postValue(value!!)
            }
        })

        binding.vidFaRefresh.setEnableLoadMore(false)

        whorlView = ViewUtils.findViewById(
            binding.vidFaState.getView(ViewAssist.TYPE_ING),
            R.id.vid_sli_load_view
        )
        // 设置监听
        binding.vidFaState.setListener(object : StateLayout.Listener {
            override fun onRemove(
                layout: StateLayout,
                type: Int,
                removeView: Boolean
            ) {
            }

            override fun onNotFound(
                layout: StateLayout,
                type: Int
            ) {
                if (type == ViewAssist.TYPE_SUCCESS) {
                    ViewUtils.reverseVisibilitys(true, binding.vidFaRefresh, binding.vidFaState)
                    whorlView?.stop()
                    binding.vidFaRefresh.finishRefresh()
                }
            }

            override fun onChange(
                layout: StateLayout,
                type: Int,
                oldType: Int,
                view: View
            ) {
                if (ViewUtils.reverseVisibilitys(
                        type == ViewAssist.TYPE_SUCCESS,
                        binding.vidFaRefresh, binding.vidFaState
                    )
                ) {
                    whorlView?.stop()
                    binding.vidFaRefresh.finishRefresh()
                } else {
                    if (type == ViewAssist.TYPE_ING) {
                        if (whorlView != null && !whorlView!!.isCircling()) {
                            whorlView?.start()
                        }
                    } else {
                        whorlView?.stop()
                        // 无数据处理
                        if (type == ViewAssist.TYPE_EMPTY_DATA) {
                            binding.vidFaRefresh.finishRefresh()
                            var tips = if (searchContent.isEmpty()) {
                                ResourceUtils.getString(R.string.str_search_noresult_tips_1)
                            } else {
                                ResourceUtils.getString(
                                    R.string.str_search_noresult_tips,
                                    HtmlUtils.addHtmlColor(searchContent, "#359AFF")
                                )
                            }
                            TextViewUtils.setHtmlText(
                                view.findViewById<TextView>(R.id.vid_slnd_tips_tv), tips
                            )
                        }
                    }
                }
            }
        })
        binding.vidFaState.showIng()

        // 设置刷新事件
        binding.vidFaRefresh.setOnRefreshListener(OnRefreshListener {
            type?.let { requestReadWrite(true) }
        })

        var itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
                    var adapter: ApkListAdapter? = binding.vidFaRefresh.getAdapter()
                    try {
                        val position = viewHolder.adapterPosition
                        FileUtils.deleteFile(adapter?.getItem(position)?.uri)
                        adapter?.removeAt(position)
                    } catch (e: Exception) {
                        DevLogger.e(e)
                    }
                    adapter?.let {
                        if (it.getDefItemCount() == 0) {
                            binding.vidFaState.showEmptyData()
                        }
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.vidFaRefresh.getRecyclerView())
    }

    override fun initObserve() {
        super.initObserve()

        // 搜索监听
        viewModel.search.observe(this) {
            when (it.action) {
                ActionEnum.COLLAPSE -> { // 搜索合并
                    if (it.type == type) {
                        if (searchContent.isNotEmpty()) { // 输入内容才刷新列表
                            searchContent = ""
                            requestReadWrite()
                        }
                    }
                }
                ActionEnum.EXPAND -> { // 搜索展开
                }
                ActionEnum.CONTENT -> { // 搜索输入内容
                    if (it.type == type) {
                        searchContent = it.content
                        requestReadWrite()
                    }
                }
            }
        }
        // Fragment 切换监听
        viewModel.fragmentChange.observe(this) {
            if (it == type) {
                searchContent = "" // 切换 Fragment 重置搜索内容
                requestReadWrite()
            }
        }
        // 回到顶部
        viewModel.backTop.observe(this) {
            if (it == dataStore.typeEnum) {
                ListViewUtils.smoothScrollToTop(binding.vidFaRefresh.getRecyclerView())
            }
        }
        // 刷新操作
        viewModel.refresh.observe(this) {
            if (it == dataStore.typeEnum) {
                binding.vidFaRefresh.getRefreshLayout()?.autoRefresh()
            }
        }
        // 文件删除
        viewModel.fileDelete.observe(this) {
            binding.vidFaRefresh.getRecyclerView()?.adapter?.notifyDataSetChanged()
        }
        // 文件扫描
        viewModel.sdcardScan.observe(this) {
            var lists = if (searchContent.isEmpty()) {
                it
            } else {
                AppSearchUtils.filterApkList(it, searchContent)
            }
            if (lists.isEmpty()) {
                binding.vidFaState.showEmptyData()
            } else {
                binding.vidFaRefresh.setAdapter(ApkListAdapter(lists))
                binding.vidFaState.showSuccess()
            }
        }
    }

    /**
     * 请求读写权限
     */
    private fun requestReadWrite() {
        requestReadWrite(false)
    }

    /**
     * 请求读写权限
     * @param refresh 是否刷新
     */
    private fun requestReadWrite(refresh: Boolean) {
        PermissionUtils.permission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).callback(object :
            PermissionUtils.PermissionCallback {
            override fun onGranted() {
                ScanSDCardUtils.instance.query(refresh) // 扫描 SDCard
            }

            override fun onDenied(
                grantedList: List<String>,
                deniedList: List<String>,
                notFoundList: List<String>
            ) {
                var builder = StringBuilder()
                    .append("申请通过的权限").append(Arrays.toString(grantedList.toTypedArray()))
                    .append(DevFinal.NEW_LINE_STR)
                    .append("拒绝的权限").append(deniedList.toString())
                    .append(DevFinal.NEW_LINE_STR)
                    .append("未找到的权限").append(notFoundList.toString())
                if (deniedList.isNotEmpty()) {
                    DevLogger.d(builder.toString())
                    ToastTintUtils.info(ResourceUtils.getString(R.string.str_read_write_request_tips))
                } else {
                    onGranted()
                }
            }
        }).request(activity)
    }
}