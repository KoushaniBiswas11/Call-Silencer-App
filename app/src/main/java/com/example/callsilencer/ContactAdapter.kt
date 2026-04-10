package com.example.callsilencer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contacts: MutableList<ContactItem>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactName: TextView = view.findViewById(R.id.contactName)
        val contactPhone: TextView = view.findViewById(R.id.contactPhone)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.contactName.text = contact.name
        holder.contactPhone.text = contact.phone

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun removeAt(position: Int) {
        if (position in 0 until contacts.size) {
            contacts.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}