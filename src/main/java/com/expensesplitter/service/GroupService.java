package com.expensesplitter.service;

import com.expensesplitter.dao.GroupDao;
import com.expensesplitter.dao.NotificationDao;
import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.GroupMember;
import com.expensesplitter.model.Notification;
import com.expensesplitter.model.User;

import java.util.List;
import java.util.Optional;

public class GroupService {

    private final GroupDao groupDao = new GroupDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();

    public List<Group> getUserGroups(int userId) {
        return groupDao.findGroupsByUserId(userId);
    }

    public Optional<Group> getGroupById(int groupId) {
        return groupDao.findById(groupId);
    }

    public boolean createGroup(String name, String description, int creatorUserId, List<Integer> memberUserIds) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required.");
        }

        Group group = new Group();
        group.setName(name.trim());
        group.setDescription(description != null ? description.trim() : "");
        group.setCreatedByUserId(creatorUserId);

        boolean created = groupDao.create(group, memberUserIds);
        if (created && memberUserIds != null) {
            for (int uid : memberUserIds) {
                if (uid != creatorUserId) {
                    Notification n = new Notification(
                            0, uid, "Added to Group",
                            "You have been added to the group: " + group.getName(),
                            "GROUP_INVITE", false
                    );
                    notificationDao.create(n);
                }
            }
        }
        return created;
    }

    public boolean updateGroup(Group group) {
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required.");
        }
        return groupDao.update(group);
    }

    public boolean deleteGroup(int groupId) {
        return groupDao.delete(groupId);
    }

    public List<GroupMember> getGroupMembers(int groupId) {
        return groupDao.getMembers(groupId);
    }

    public boolean addMember(int groupId, int userId, String role) {
        boolean ok = groupDao.addMember(groupId, userId, role);
        if (ok) {
            Optional<Group> g = groupDao.findById(groupId);
            g.ifPresent(group -> {
                Notification n = new Notification(
                        0, userId, "Added to Group",
                        "You were added to " + group.getName(),
                        "GROUP_INVITE", false
                );
                notificationDao.create(n);
            });
        }
        return ok;
    }

    public boolean removeMember(int groupId, int userId) {
        return groupDao.removeMember(groupId, userId);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }
}

