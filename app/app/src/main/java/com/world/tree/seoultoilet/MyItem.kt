package com.world.tree.seoultoilet

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class MyItem (val _postion: LatLng, val _title: String, val _snippet: String, val _icon: BitmapDescriptor)
    : ClusterItem {
    override fun getSnippet(): String {
        return _snippet
    }

    override fun getTitle(): String {
        return _title
    }

    override fun getPosition(): LatLng {
        return _postion
    }

    fun getIcon(): BitmapDescriptor {
        return _icon
    }

    override fun equals(other: Any?): Boolean {
        if (other is MyItem) {
            return (other.position.latitude == position.latitude
                    && other.position.longitude == position.longitude
                    && other.title == _title
                    && other.snippet == _snippet)
        }
        return false
    }

    override fun hashCode(): Int {
        var hash = _postion.latitude.hashCode() * 31
        hash = hash * 31 + _postion.longitude.hashCode()
        hash = hash * 31 + title.hashCode()
        hash = hash * 31 + snippet.hashCode()
        return hash
    }
}