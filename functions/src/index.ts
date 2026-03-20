// EduSoul/functions/src/index.ts

import * as admin from "firebase-admin";
import {
  onDocumentCreated,
  onDocumentDeleted,
  QueryDocumentSnapshot,
  FirestoreEvent,
} from "firebase-functions/v2/firestore";

admin.initializeApp();
const db = admin.firestore();

async function deleteCollectionInBatches(
  collectionRef: admin.firestore.Query,
  batchSize = 200
): Promise<void> {
  const query = collectionRef.limit(batchSize);
  return new Promise((resolve, reject) => {
    deleteQueryBatch(query, resolve as (value?: unknown) => void).catch(reject);
  });
}

async function deleteQueryBatch(
  query: admin.firestore.Query,
  resolve: (value?: unknown) => void
): Promise<void> {
  const snapshot = await query.get();

  if (snapshot.size === 0) {
    resolve(null);
    return;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.delete(doc.ref);
  });

  await batch.commit();

  process.nextTick(() => {
    deleteQueryBatch(query, resolve);
  });
}

async function updateCollectionInBatches(
  collectionRef: admin.firestore.Query,
  updateData: Record<string, any>,
  batchSize = 200
): Promise<void> {
  const query = collectionRef.limit(batchSize);
  return new Promise((resolve, reject) => {
    updateQueryBatch(query, updateData, resolve as (value?: unknown) => void)
      .catch(reject);
  });
}

async function updateQueryBatch(
  query: admin.firestore.Query,
  updateData: Record<string, any>,
  resolve: (value?: unknown) => void
): Promise<void> {
  const snapshot = await query.get();

  if (snapshot.size === 0) {
    resolve(null);
    return;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.update(doc.ref, updateData);
  });

  await batch.commit();

  process.nextTick(() => {
    updateQueryBatch(query, updateData, resolve);
  });
}

export const onDeleteBatch = onDocumentDeleted(
  "batches/{batchId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const deletedBatchId = event.params.batchId;
    console.log(`Triggered onDeleteBatch for batch: ${deletedBatchId}`);

    try {
      await deleteCollectionInBatches(
        db.collection("class_sessions").where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted class_sessions for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("recurring_class_sessions")
          .where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted recurring_class_sessions for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("student_enrollments")
          .where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted student_enrollments for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("teacher_assignments")
          .where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted teacher_assignments for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("syllabus_progress").where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted syllabus_progress for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("learning_resources")
          .where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted learning_resources for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("exams").where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted exams for batch: ${deletedBatchId}`);

      await deleteCollectionInBatches(
        db.collection("batch_assignments").where("batchId", "==", deletedBatchId)
      );
      console.log(`Deleted batch_assignments for batch: ${deletedBatchId}`);

      console.log(`Successfully cascaded deletion for batch: ${deletedBatchId}`);
    } catch (error: unknown) {
      console.error(`Error cascading deletion for batch ${deletedBatchId}:`, error);
    }
  }
);


export const onDeleteStudent = onDocumentDeleted(
  "students/{studentId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const deletedStudentId = event.params.studentId;
    console.log(`Triggered onDeleteStudent for student: ${deletedStudentId}`);

    try {
      await deleteCollectionInBatches(
        db.collection("attendance").where("studentId", "==", deletedStudentId)
      );
      console.log(`Deleted attendance records for student: ${deletedStudentId}`);

      await deleteCollectionInBatches(
        db.collection("results").where("studentId", "==", deletedStudentId)
      );
      console.log(`Deleted results for student: ${deletedStudentId}`);

      await deleteCollectionInBatches(
        db.collection("student_assignments")
          .where("studentId", "==", deletedStudentId)
      );
      console.log(`Deleted student_assignments for student: ${deletedStudentId}`);

      await deleteCollectionInBatches(
        db.collection("feePayments").where("studentId", "==", deletedStudentId)
      );
      console.log(`Deleted feePayments for student: ${deletedStudentId}`);

      await deleteCollectionInBatches(
        db.collection("student_enrollments")
          .where("studentId", "==", deletedStudentId)
      );
      console.log(`Deleted student_enrollments for student: ${deletedStudentId}`);

      console.log(`Successfully cascaded deletion for student: ${deletedStudentId}`);
    } catch (error: unknown) {
      console.error(`Error cascading deletion for student ${deletedStudentId}:`, error);
    }
  }
);

export const onDeleteSubject = onDocumentDeleted(
  "subjects/{subjectId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const deletedSubjectId = event.params.subjectId;
    console.log(`Triggered onDeleteSubject for subject: ${deletedSubjectId}`);

    try {
      await deleteCollectionInBatches(
        db.collection("syllabus_topics").where("subjectId", "==", deletedSubjectId)
      );
      console.log(`Deleted syllabus_topics for subject: ${deletedSubjectId}`);

      await deleteCollectionInBatches(
        db.collection("exams").where("subjectId", "==", deletedSubjectId)
      );
      console.log(`Deleted exams for subject: ${deletedSubjectId}`);

      await deleteCollectionInBatches(
        db.collection("homework").where("subjectId", "==", deletedSubjectId)
      );
      console.log(`Deleted homework for subject: ${deletedSubjectId}`);

      await deleteCollectionInBatches(
        db.collection("learning_resources")
          .where("subjectId", "==", deletedSubjectId)
      );
      console.log(`Deleted learning_resources for subject: ${deletedSubjectId}`);

      await deleteCollectionInBatches(
        db.collection("teacher_assignments")
          .where("subjectId", "==", deletedSubjectId)
      );
      console.log(`Deleted teacher_assignments for subject: ${deletedSubjectId}`);

      console.log(`Successfully cascaded deletion for subject: ${deletedSubjectId}`);
    } catch (error: unknown) {
      console.error(`Error cascading deletion for subject ${deletedSubjectId}:`, error);
    }
  }
);

export const onDeleteUser = onDocumentDeleted(
  "users/{userId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const deletedUserId = event.params.userId;
    const deletedUserRole = event.data?.data()?.role;
    console.log(
      `Triggered onDeleteUser for user: ${deletedUserId} (Role: ${deletedUserRole})`
    );

    try {
      await deleteCollectionInBatches(
        db.collection("messages").where("senderId", "==", deletedUserId)
      );
      console.log(`Deleted sent messages for user: ${deletedUserId}`);
      await deleteCollectionInBatches(
        db.collection("messages").where("receiverId", "==", deletedUserId)
      );
      console.log(`Deleted received messages for user: ${deletedUserId}`);

      if (deletedUserRole === "TEACHER" ||
          deletedUserRole === "ADMIN" ||
          deletedUserRole === "OWNER") {
        await deleteCollectionInBatches(
          db.collection("teacher_assignments")
            .where("teacherUserId", "==", deletedUserId)
        );
        console.log(`Deleted teacher_assignments for teacher: ${deletedUserId}`);

        await deleteCollectionInBatches(
          db.collection("class_sessions")
            .where("teacherUserId", "==", deletedUserId)
        );
        console.log(`Deleted class_sessions for teacher: ${deletedUserId}`);

        await deleteCollectionInBatches(
          db.collection("homework").where("teacherId", "==", deletedUserId)
        );
        console.log(`Deleted homework for teacher: ${deletedUserId}`);

        await deleteCollectionInBatches(
          db.collection("learning_resources")
            .where("uploadedByTeacherId", "==", deletedUserId)
        );
        console.log(`Deleted learning_resources for teacher: ${deletedUserId}`);

        await updateCollectionInBatches(
          db.collection("announcements")
            .where("authorUserId", "==", deletedUserId),
          {authorUserId: null, authorName: "Deleted User"}
        );
        console.log(`Updated announcements authored by teacher: ${deletedUserId}`);

        await updateCollectionInBatches(
          db.collection("results").where("enteredByUserId", "==", deletedUserId),
          {enteredByUserId: null}
        );
        console.log(`Updated results entered by teacher: ${deletedUserId}`);

        await updateCollectionInBatches(
          db.collection("syllabus_progress")
            .where("updatedByTeacherId", "==", deletedUserId),
          {updatedByTeacherId: null}
        );
        console.log(`Updated syllabus_progress by teacher: ${deletedUserId}`);
      }

      if (deletedUserRole === "PARENT" ||
          deletedUserRole === "ADMIN" ||
          deletedUserRole === "OWNER") {
        await updateCollectionInBatches(
          db.collection("students").where("parentUserId", "==", deletedUserId),
          {parentUserId: null}
        );
        console.log(`Updated students linked to parent: ${deletedUserId}`);
      }

      console.log(`Successfully cascaded deletion for user: ${deletedUserId}`);
    } catch (error: unknown) {
      console.error(`Error cascading deletion for user ${deletedUserId}:`, error);
    }
  }
);

export const sendChatMessageNotification = onDocumentCreated(
  "messages/{messageId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const message = event.data?.data();
    if (!message) {
      console.log("No message data found in event.");
      return;
    }

    const senderId = message.senderId;
    const receiverId = message.receiverId;
    const messageContent = message.messageContent;

    if (!senderId || !receiverId || !messageContent) {
      console.log("Missing senderId, receiverId, or messageContent in message.");
      return;
    }

    try {
      const senderDoc = await db.collection("users").doc(senderId).get();
      const senderData = senderDoc.data();
      const senderName = senderData?.fullName || senderData?.username || "Unknown Sender";

      const receiverDoc = await db.collection("users").doc(receiverId).get();
      const receiverData = receiverDoc.data();
      const receiverFcmToken = receiverData?.fcmToken;

      if (!receiverFcmToken) {
        console.log(
          `No FCM token found for receiver: ${receiverId}. Cannot send notification.`
        );
        return;
      }

      const payload: admin.messaging.Message = {
        token: receiverFcmToken,
        data: {
          senderId: senderId,
          receiverId: receiverId,
          messageContent: messageContent,
          senderName: senderName,
        },
      };

      const response = await admin.messaging().send(payload);
      console.log("Successfully sent message:", response);
    } catch (error: unknown) {
      if (error instanceof Error) {
        console.error(
          `Error sending notification for message ${event.params.messageId}:`,
          error.message
        );
        if (
          (error as { code?: string }).code === "messaging/invalid-argument" ||
          (error as { code?: string }).code ===
            "messaging/registration-token-not-registered"
        ) {
          console.log(`Removing invalid/expired FCM token for user ${receiverId}`);
          await db
            .collection("users")
            .doc(receiverId)
            .update({ fcmToken: admin.firestore.FieldValue.delete() });
        }
      } else {
        console.error(
          `An unknown error occurred sending notification for message ` +
            `${event.params.messageId}:`,
          error
        );
      }
    }
  }
);

export const onInstituteAttendanceMarked = onDocumentCreated(
  "instituteAttendance/{attendanceId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
        const attendanceData = event.data?.data();
        const attendanceId = event.params.attendanceId;

        if (!attendanceData) {
            console.log("No data found for new institute attendance document:", attendanceId);
            return null;
        }

        const studentId = attendanceData.studentId;
        const markedAtTimestamp = attendanceData.markedAt;
        const markedByUserId = attendanceData.markedByUserId;

        if (!studentId || !markedAtTimestamp) {
            console.warn(`Missing studentId or markedAt for attendance record ${attendanceId}. Skipping notification.`);
            return null;
        }

        try {
            const studentDoc = await db.collection("students").doc(studentId).get();
            if (!studentDoc.exists) {
                console.warn(`Student with ID ${studentId} not found for attendance record ${attendanceId}. Skipping notification.`);
                return null;
            }
            const studentData = studentDoc.data();
            const parentUserId = studentData?.parentUserId;
            const studentName = studentData?.fullName || studentData?.username || "a student";

            if (!parentUserId) {
                console.warn(`Parent User ID not found for student ${studentId}. Skipping notification.`);
                return null;
            }

            const parentUserDoc = await db.collection("users").doc(parentUserId).get();
            if (!parentUserDoc.exists) {
                console.warn(`Parent user with ID ${parentUserId} not found. Skipping notification.`);
                return null;
            }
            const parentUserData = parentUserDoc.data();
            const parentFcmToken = parentUserData?.fcmToken;

            if (!parentFcmToken) {
                console.warn(`FCM token not found for parent ${parentUserId}. Skipping notification.`);
                return null;
            }

            let managerName = "The institute";
            if (markedByUserId) {
                const managerUserDoc = await db.collection("users").doc(markedByUserId).get();
                if (managerUserDoc.exists) {
                    managerName = managerUserDoc.data()?.fullName || managerUserDoc.data()?.username || "A manager";
                }
            }

            const arrivalDate = new Date(markedAtTimestamp);
            const arrivalTime = arrivalDate.toLocaleTimeString("en-IN", {
                hour: "2-digit",
                minute: "2-digit",
                hour12: true,
                timeZone: "Asia/Kolkata"
            });
            const arrivalDateFormatted = arrivalDate.toLocaleDateString("en-IN", {
                year: 'numeric', month: 'long', day: 'numeric',
                timeZone: "Asia/Kolkata"
            });


            const notificationTitle = "Child Arrived at Institute!";
            const notificationBody = `Your child, ${studentName}, has arrived at the institute at ${arrivalTime} on ${arrivalDateFormatted}, marked by ${managerName}.`;

            const message = {
                notification: {
                    title: notificationTitle,
                    body: notificationBody,
                },
                token: parentFcmToken,
                data: {
                    type: "institute_arrival",
                    studentId: studentId,
                    attendanceId: attendanceId,
                    parentUserId: parentUserId
                }
            };

            await admin.messaging().send(message);
            console.log(`FCM notification sent to parent ${parentUserId} for student ${studentId}.`);

            const messageContent = `Your child, ${studentName}, arrived at the institute at ${arrivalTime} on ${arrivalDateFormatted}, marked by ${managerName}.`;
            const newMessageRef = db.collection("messages").doc();
            await newMessageRef.set({
                id: newMessageRef.id,
                senderId: "system",
                receiverId: parentUserId,
                content: messageContent,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                read: false,
                type: "notification"
            });
            console.log(`In-app message recorded for parent ${parentUserId}.`);

            return { success: true };

        } catch (error: unknown) {
            console.error(`Error processing institute attendance record ${attendanceId}:`, error);
            return { error: (error as Error).message };
        }
    }
);

export const sendAnnouncementNotification = onDocumentCreated(
  "announcements/{announcementId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined>) => {
    const announcement = event.data?.data();
    if (!announcement) {
      console.log("No announcement data found in event.");
      return;
    }

    const { title, content, targetAudience, authorName } = announcement;

    if (!title || !content || !targetAudience) {
      console.log("Missing title, content, or targetAudience in announcement.");
      return;
    }

    let usersQuery: admin.firestore.Query;
    let notificationBodyPrefix = "";

    switch (targetAudience) {
      case "ALL":
        usersQuery = db.collection("users");
        notificationBodyPrefix = "A new general announcement: ";
        break;
      case "TEACHERS":
        usersQuery = db.collection("users").where("role", "==", "TEACHER");
        notificationBodyPrefix = "New teacher announcement: ";
        break;
      case "PARENTS":
        usersQuery = db.collection("users").where("role", "==", "PARENT");
        notificationBodyPrefix = "New parent announcement: ";
        break;
      default:
        console.log(`Announcement target audience '${targetAudience}' not supported for automatic notifications. Skipping.`);
        return;
    }

    try {
      const usersSnapshot = await usersQuery.get();
      const tokens: string[] = [];
      const userIds: string[] = [];

      usersSnapshot.docs.forEach((doc) => {
        const userData = doc.data();
        if (userData.fcmToken) {
          tokens.push(userData.fcmToken);
          userIds.push(doc.id);
        }
      });

      if (tokens.length === 0) {
        console.log(`No FCM tokens found for target audience '${targetAudience}'. No notifications sent.`);
        return;
      }

      const notificationTitle = `New Announcement: ${title}`;
      const notificationBody = `${notificationBodyPrefix}${content.substring(0, 100)}${content.length > 100 ? "..." : ""}`;

      const message: admin.messaging.MulticastMessage = {
        tokens: tokens,
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          type: "announcement",
          announcementId: event.params.announcementId,
          targetAudience: targetAudience,
          title: title,
          content: content,
          authorName: authorName || "EduSoul Admin",
        },
      };

      const response = await admin.messaging().sendEachForMulticast(message);
      console.log(`Successfully sent announcement notifications to ${response.successCount} devices (failed: ${response.failureCount}).`);

      if (response.failureCount > 0) {
        response.responses.forEach(async (resp, idx) => {
          if (!resp.success) {
            console.error(`Failed to send notification to ${userIds[idx]} (token: ${tokens[idx]}): ${resp.error?.message}`);
            if (resp.error?.code === "messaging/invalid-argument" || resp.error?.code === "messaging/registration-token-not-registered") {
              console.log(`Removing invalid/expired FCM token for user ${userIds[idx]}`);
              await db.collection("users").doc(userIds[idx]).update({ fcmToken: admin.firestore.FieldValue.delete() });
            }
          }
        });
      }

    } catch (error: unknown) {
      console.error(`Error sending announcement notification for announcement ${event.params.announcementId}:`, error);
    }
  }
);