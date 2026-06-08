package com.danielschatz.gliderweightbalance.adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class DragManageAdapter(private val adapter: ItemMoveCallback) : ItemTouchHelper.Callback() {

    // Deaktiviere das Ziehen durch langes Drücken überall auf der Karte
    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        // "Add Button" (letztes Item) darf nicht bewegt werden
        if (viewHolder.bindingAdapterPosition == (recyclerView.adapter?.itemCount ?: 0) - 1) {
            return makeMovementFlags(0, 0)
        }
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        // Verhindere, dass man über den "Add Button" hinaus schiebt
        if (target.bindingAdapterPosition == (recyclerView.adapter?.itemCount ?: 0) - 1) return false

        adapter.onRowMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}
