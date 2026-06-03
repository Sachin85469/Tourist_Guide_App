package com.arriva.touristguideapp.sos.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.R
import com.arriva.touristguideapp.data.sos.SOSContact

class EmergencyContactAdapter(
    private var contacts: List<SOSContact>,
    private val onEditClick: (SOSContact) -> Unit,
    private val onDeleteClick: (SOSContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    fun updateContacts(newContacts: List<SOSContact>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_emergency_contact,
            parent,
            false
        )
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvRelationship: TextView = itemView.findViewById(R.id.tv_contact_relationship)
        private val tvPhone: TextView = itemView.findViewById(R.id.tv_contact_phone)
        private val ivEdit: ImageView = itemView.findViewById(R.id.iv_edit_contact)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete_contact)

        fun bind(contact: SOSContact) {
            tvName.text = contact.name
            tvRelationship.text = contact.relationship
            tvPhone.text = contact.phone

            if (contact.relationship.isEmpty()) {
                tvRelationship.visibility = View.GONE
            } else {
                tvRelationship.visibility = View.VISIBLE
                tvRelationship.text = contact.relationship
            }

            ivEdit.setOnClickListener { onEditClick(contact) }
            ivDelete.setOnClickListener { onDeleteClick(contact) }
        }
    }
}
