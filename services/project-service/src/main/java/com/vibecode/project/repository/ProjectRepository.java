package com.vibecode.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.vibecode.project.entity.Project;


public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findByOwnerId(UUID ownerId);

}
