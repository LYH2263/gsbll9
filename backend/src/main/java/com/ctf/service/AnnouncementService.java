package com.ctf.service;

import com.ctf.entity.Announcement;
import com.ctf.mapper.AnnouncementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    public List<Announcement> getAllAnnouncements() {
        return announcementMapper.selectAll();
    }

    public List<Announcement> getActiveAnnouncements() {
        return announcementMapper.selectActive();
    }

    public Announcement getLatestActiveAnnouncement() {
        return announcementMapper.selectLatestActive();
    }

    public Announcement getById(Integer id) {
        return announcementMapper.selectById(id);
    }

    @Transactional
    public Announcement createAnnouncement(Announcement announcement) {
        if (announcement.getIsActive() == null) {
            announcement.setIsActive(false);
        }
        announcementMapper.insert(announcement);
        log.info("Announcement created: id={}, content={}", announcement.getId(), 
                announcement.getContent().length() > 50 
                        ? announcement.getContent().substring(0, 50) + "..." 
                        : announcement.getContent());
        return announcement;
    }

    @Transactional
    public Announcement updateAnnouncement(Integer id, Announcement announcement) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            return null;
        }
        announcement.setId(id);
        announcementMapper.update(announcement);
        log.info("Announcement updated: id={}", id);
        return announcementMapper.selectById(id);
    }

    @Transactional
    public boolean deleteAnnouncement(Integer id) {
        int count = announcementMapper.delete(id);
        log.info("Announcement deleted: id={}, deleted={}", id, count > 0);
        return count > 0;
    }

    @Transactional
    public Announcement publishAnnouncement(Integer id) {
        announcementMapper.deactivateAll();
        announcementMapper.activate(id);
        log.info("Announcement published: id={}", id);
        return announcementMapper.selectById(id);
    }

    @Transactional
    public boolean withdrawAnnouncement(Integer id) {
        int count = announcementMapper.deactivate(id);
        log.info("Announcement withdrawn: id={}, withdrawn={}", id, count > 0);
        return count > 0;
    }
}
