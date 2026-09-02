package com.cinebook.repository;

import com.cinebook.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByHallId(Long hallId);
    boolean existsByHallId(Long hallId);
}
