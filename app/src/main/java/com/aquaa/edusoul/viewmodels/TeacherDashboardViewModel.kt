package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
import com.aquaa.edusoul.models.*
import com.aquaa.edusoul.repositories.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.combine
import android.util.Log
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(
    private val classSessionRepository: ClassSessionRepository,
    private val homeworkRepository: HomeworkRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _teacherId = MutableLiveData<String>()

    val assignedStudentsCount: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching assignedStudentsCount for teacherId: $teacherId")
            try {
                val links: List<TeacherSubjectBatchLink> = teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Received ${links.size} TeacherSubjectBatchLink entries for assignedStudentsCount. Links data: ${links.joinToString { "Sub:${it.subjectId}, Batch:${it.batchId}" }}")
                val studentIds = links.flatMap { link: TeacherSubjectBatchLink ->
                    val studentsForBatch: List<Student>? = studentRepository.getStudentsByBatch(link.batchId).firstOrNull()
                    studentsForBatch?.map { student: Student -> student.id } ?: emptyList()
                }.toSet()
                Log.e("TeacherDashboardVM", "Calculated ${studentIds.size} unique student IDs for assignedStudentsCount.")
                emit(studentIds.size)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching assigned student count: ${e.message}", e)
                emit(0)
            }
        }
    }

    val assignedSubjectsCount: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching assignedSubjectsCount for teacherId: $teacherId")
            try {
                val links: List<TeacherSubjectBatchLink> = teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Received ${links.size} TeacherSubjectBatchLink entries for assignedSubjectsCount. Links data: ${links.joinToString { "Sub:${it.subjectId}" }}")
                emit(links.map { it.subjectId }.toSet().size)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching assigned subject count: ${e.message}", e)
                emit(0)
            }
        }
    }

    val assignedBatchesCount: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching assignedBatchesCount for teacherId: $teacherId")
            try {
                val links: List<TeacherSubjectBatchLink> = teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Received ${links.size} TeacherSubjectBatchLink entries for assignedBatchesCount. Links data: ${links.joinToString { "Batch:${it.batchId}" }}")
                emit(links.map { it.batchId }.toSet().size)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching assigned batch count: ${e.message}", e)
                emit(0)
            }
        }
    }

    val nextClassSession: LiveData<ClassSessionDetails?> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching nextClassSession for teacherId: $teacherId")
            try {
                if (teacherId.isBlank()) {
                    emit(null)
                    return@liveData
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                val todayDate = dateFormat.format(Date())
                val nowTime = timeFormat.format(Date())

                // 1. Try to find the next session today (after current time)
                val sessionsToday = classSessionRepository.getScheduledSessionsForTeacher(teacherId, todayDate).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Found ${sessionsToday.size} sessions for today for teacher: $teacherId. Current time: $nowTime")

                val nextSessionToday = sessionsToday
                    .filter { session ->
                        val isUpcoming = session.startTime > nowTime
                        Log.e("TeacherDashboardVM", "Session ${session.id} at ${session.startTime}. Is upcoming: $isUpcoming (Current time: $nowTime)")
                        isUpcoming
                    }
                    .minByOrNull { session -> session.startTime } // CORRECTED: minByOrOrNull -> minByOrNull

                if (nextSessionToday != null) {
                    Log.e("TeacherDashboardVM", "Next session found for TODAY: ${nextSessionToday.subjectId} at ${nextSessionToday.startTime}")
                    val subjectName = subjectRepository.getSubjectById(nextSessionToday.subjectId)?.subjectName ?: "Unknown Subject"
                    val batchName = batchRepository.getBatchById(nextSessionToday.batchId)?.batchName ?: "Unknown Batch"
                    val teacherName = userRepository.getUserById(nextSessionToday.teacherUserId)?.fullName ?: "Unknown Teacher"
                    emit(ClassSessionDetails(nextSessionToday, subjectName, batchName, teacherName))
                } else {
                    Log.e("TeacherDashboardVM", "No upcoming sessions found for teacher: $teacherId today. Checking future dates.")
                    // 2. If no upcoming sessions today, look for the next session in the coming days
                    val calendar = Calendar.getInstance()
                    // Start checking from tomorrow
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    var nextSessionFuture: ClassSession? = null

                    // Check for sessions in the next 7 days (or adjust as needed)
                    for (i in 0 until 7) {
                        val futureDate = dateFormat.format(calendar.time)
                        val sessionsFutureDay = classSessionRepository.getScheduledSessionsForTeacher(teacherId, futureDate).firstOrNull() ?: emptyList()
                        if (sessionsFutureDay.isNotEmpty()) {
                            nextSessionFuture = sessionsFutureDay.minByOrNull { session -> session.startTime } // CORRECTED: minByOrOrNull -> minByOrNull
                            if (nextSessionFuture != null) {
                                Log.e("TeacherDashboardVM", "Next session found on FUTURE date ${futureDate}: ${nextSessionFuture.subjectId} at ${nextSessionFuture.startTime}")
                                break // Found the earliest session in future days
                            }
                        }
                        calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to the next day
                    }

                    if (nextSessionFuture != null) {
                        val subjectName = subjectRepository.getSubjectById(nextSessionFuture.subjectId)?.subjectName ?: "Unknown Subject"
                        val batchName = batchRepository.getBatchById(nextSessionFuture.batchId)?.batchName ?: "Unknown Batch"
                        val teacherName = userRepository.getUserById(nextSessionFuture.teacherUserId)?.fullName ?: "Unknown Teacher"
                        emit(ClassSessionDetails(nextSessionFuture, subjectName, batchName, teacherName))
                    } else {
                        Log.e("TeacherDashboardVM", "No upcoming sessions found for teacher: $teacherId in the next 7 days.")
                        emit(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching next class session: ${e.message}", e)
                emit(null)
            }
        }
    }

    val pendingHomeworkToGradeCount: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching pendingHomeworkToGradeCount for teacherId: $teacherId")
            try {
                if (teacherId.isBlank()) {
                    emit(0)
                    return@liveData
                }
                val teachersHomeworks = homeworkRepository.getHomeworksByTeacher(teacherId).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Found ${teachersHomeworks.size} homeworks for teacher: $teacherId")
                val homeworkIds = teachersHomeworks.map { it.id }.toSet()

                if (homeworkIds.isEmpty()) {
                    Log.e("TeacherDashboardVM", "No homeworks assigned by teacher: $teacherId, pending count is 0.")
                    emit(0)
                    return@liveData
                }

                val allStudentAssignments = studentAssignmentRepository.getAllStudentAssignments().firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Found ${allStudentAssignments.size} total student assignments.")

                val pendingCount = allStudentAssignments.count { assignment ->
                    homeworkIds.contains(assignment.homeworkId) &&
                            assignment.status == StudentAssignment.STATUS_SUBMITTED &&
                            assignment.marksObtained == null
                }
                Log.e("TeacherDashboardVM", "Calculated $pendingCount pending homeworks to grade for teacher: $teacherId")
                emit(pendingCount)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching pending homework count: ${e.message}", e)
                emit(0)
            }
        }
    }

    val upcomingHomeworkDeadlinesCount: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching upcomingHomeworkDeadlinesCount for teacherId: $teacherId")
            try {
                if (teacherId.isBlank()) {
                    emit(0)
                    return@liveData
                }
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                val sevenDaysLater = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

                val teachersHomeworks = homeworkRepository.getHomeworksByTeacher(teacherId).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Found ${teachersHomeworks.size} homeworks for teacher: $teacherId for deadlines.")

                val upcomingDeadlines = teachersHomeworks.filter { homework ->
                    homework.dueDate >= todayDate && homework.dueDate <= sevenDaysLater
                }
                Log.e("TeacherDashboardVM", "Calculated ${upcomingDeadlines.size} upcoming homework deadlines for teacher: $teacherId")
                emit(upcomingDeadlines.size)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching upcoming homework deadlines: ${e.message}", e)
                emit(0)
            }
        }
    }


    val upcomingClassesToday: LiveData<Int> = _teacherId.switchMap { teacherId ->
        liveData(Dispatchers.IO) {
            Log.e("TeacherDashboardVM", "Fetching upcomingClassesToday (quick stats) for teacherId: $teacherId")
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val sessions = classSessionRepository.getScheduledSessionsForTeacher(teacherId, today).firstOrNull() ?: emptyList()
                Log.e("TeacherDashboardVM", "Found ${sessions.size} classes today for teacher: $teacherId")
                emit(sessions.size)
            } catch (e: Exception) {
                Log.e("TeacherDashboardVM", "Error fetching upcoming classes today (quick stats): ${e.message}", e)
                emit(0)
            }
        }
    }

    fun setTeacherId(teacherId: String) {
        Log.e("TeacherDashboardVM", "Setting teacherId: $teacherId")
        _teacherId.value = teacherId
    }
}