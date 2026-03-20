// File: main/java/com/aquaa/edusoul/adapters/UserAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.User
import java.util.ArrayList

/*
 * UserAdapter: Manages the list of users for the Admin.
 * Migrated to Kotlin.
 */
class UserAdapter(
    private val context: Context,
    private var userList: ArrayList<User>, // List of User objects
    private val listener: OnUserActionsListener? // Listener for user actions
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    interface OnUserActionsListener {
        // Changed userId parameter type from Long to String
        fun onEditUser(user: User, position: Int)
        // Changed userId parameter type from Long to String
        fun onDeleteUser(user: User, position: Int)
        // Changed userId parameter type from Long to String
        fun onAssignTeacher(user: User, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.textViewUserName.text = user.fullName
        holder.textViewUserRole.text = user.role
        holder.textViewUserEmail.text = user.email ?: "" // Display blank if null
        holder.textViewUserUsername.text = "Username: ${user.username}"


        // Set visibility of action buttons based on user role and whether a listener is provided
        val isTeacher = user.role == User.ROLE_TEACHER
        val isOwner = user.role == User.ROLE_OWNER
        val isManager = user.role == "manager"

        // Edit and delete buttons visible for all non-owner users
        holder.imageButtonEditUser.visibility = if (isOwner) View.GONE else View.VISIBLE
        holder.imageButtonDeleteUser.visibility = if (isOwner) View.GONE else View.VISIBLE

        // Assign teacher button only visible for teachers
        holder.imageButtonAssignTeacher.visibility = if (isTeacher) View.VISIBLE else View.GONE


        // Set click listeners only if a listener is provided
        listener?.let {
            if (!isOwner) { // Only enable edit/delete for non-owner users
                holder.imageButtonEditUser.setOnClickListener {
                    listener.onEditUser(user, holder.adapterPosition)
                }
                holder.imageButtonDeleteUser.setOnClickListener {
                    listener.onDeleteUser(user, holder.adapterPosition)
                }
            }
            if (isTeacher) { // Only enable assign for teachers
                holder.imageButtonAssignTeacher.setOnClickListener {
                    listener.onAssignTeacher(user, holder.adapterPosition)
                }
            }
        } ?: run {
            // If no listener, disable all click functionality visually
            holder.imageButtonEditUser.isEnabled = false
            holder.imageButtonDeleteUser.isEnabled = false
            holder.imageButtonAssignTeacher.isEnabled = false
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    fun setUsers(newList: List<User>) {
        this.userList.clear()
        this.userList.addAll(newList)
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewUserIcon: ImageView = itemView.findViewById(R.id.imageViewUserIcon)
        val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserFullName)
        val textViewUserRole: TextView = itemView.findViewById(R.id.textViewUserRole)
        val textViewUserUsername: TextView = itemView.findViewById(R.id.textViewUserUsername)
        val textViewUserEmail: TextView = itemView.findViewById(R.id.textViewUserEmail)
        val imageButtonEditUser: ImageButton = itemView.findViewById(R.id.imageButtonEditUser)
        val imageButtonDeleteUser: ImageButton = itemView.findViewById(R.id.imageButtonDeleteUser)
        val imageButtonAssignTeacher: ImageButton = itemView.findViewById(R.id.imageButtonAssignTeacher)
    }
}