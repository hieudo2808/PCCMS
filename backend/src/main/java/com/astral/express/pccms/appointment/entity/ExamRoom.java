package com.astral.express.pccms.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "exam_rooms")
@Getter
public class ExamRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
