// Import các hàm cần thiết từ các module tương ứng của Firebase v2
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { getFirestore } = require("firebase-admin/firestore");
const admin = require("firebase-admin");

// Dòng "const functions = require("firebase-functions");" đã được xóa vì không cần thiết

// Khởi tạo Firebase Admin SDK
admin.initializeApp();

// Sử dụng cú pháp v2 để định nghĩa function
// Hàm sẽ lắng nghe sự kiện cập nhật trên bất kỳ tài liệu nào trong collection 'Task'
exports.sendNotificationOnTaskAssignment = onDocumentUpdated("Task/{taskId}", async (event) => {
    // Trong v2, dữ liệu nằm trong event.data
    if (!event.data) {
        console.log("Không có dữ liệu sự kiện.");
        return;
    }

    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();

    const membersBefore = beforeData.members || [];
    const membersAfter = afterData.members || [];

    const addedMembers = membersAfter.filter(
        (memberId) => !membersBefore.includes(memberId),
    );

    if (addedMembers.length === 0) {
        console.log("Không có thành viên mới nào được thêm.");
        return;
    }

    const addedMemberIds = addedMembers.join(", ");
    console.log(`Phát hiện thành viên mới: ${addedMemberIds}`);

    const db = getFirestore();

    for (const userId of addedMembers) {
        try {
            const userDoc = await db.collection("User").doc(userId).get();

            if (!userDoc.exists) {
                console.log(`Không tìm thấy tài liệu cho userId: ${userId}`);
                continue;
            }

            const userData = userDoc.data();
            const fcmToken = userData.fcmToken;

            if (fcmToken) {
                const payload = {
                    notification: {
                        title: "Bạn có công việc mới!",
                        body: `Bạn được thêm vào công việc: '${afterData.title}'`,
                    },
                };

                console.log(`Đang gửi thông báo đến người dùng: ${userId}`);
                await admin.messaging().sendToDevice(fcmToken, payload);
            }
        } catch (error) {
            console.error(`Lỗi khi gửi thông báo cho user ${userId}:`, error);
        }
    }
});
