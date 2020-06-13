package com.world.tree.seoultoilet

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class ClusterRenderer(context: Context?, map: GoogleMap?, clusterManager: ClusterManager<MyItem>?)
    :DefaultClusterRenderer<MyItem>(context, map, clusterManager) {

    init {
        // 전달받은 clusterManager 객체에 render를 자신으로 지정
        clusterManager?.renderer = this
    }


    // 클러스터 아이템이 렌더링 되기전 호출되는 함수
    override fun onBeforeClusterItemRendered(item: MyItem?, markerOptions: MarkerOptions?) {
        // 마커의 아이콘 지정
        markerOptions?.icon(item?._icon)
        markerOptions?.visible(true)
    }
}