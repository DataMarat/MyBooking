package com.mybooking.hotelservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность отеля.
 *
 * <p>Описывает отель как агрегат доменной модели, включающий базовую информацию
 * об отеле и связанные с ним номера.</p>
 */
@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название отеля.
     */
    private String name;

    /**
     * Адрес отеля.
     */
    private String address;

    /**
     * Город отеля.
     */
    private String city;
    /**
     * Список номеров, принадлежащих отелю.
     *
     * <p>Связь один-ко-многим. Загрузка происходит лениво.
     * Все операции жизненного цикла каскадируются на номера.</p>
     */
    @OneToMany(mappedBy = "hotel",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

//    protected Hotel() {
//        // Конструктор без аргументов требуется JPA
//    }

//    public Hotel(String name, String address) {
//        this.name = name;
//        this.address = address;
//    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

//    /**
//     * Добавляет номер в отель и синхронизирует двустороннюю связь.
//     *
//     * @param room номер отеля
//     */
//    public void addRoom(Room room) {
//        rooms.add(room);
//        room.setHotel(this);
//    }
//
//    /**
//     * Удаляет номер из отеля и синхронизирует двустороннюю связь.
//     *
//     * @param room номер отеля
//     */
//    public void removeRoom(Room room) {
//        rooms.remove(room);
//        room.setHotel(null);
//    }
}
