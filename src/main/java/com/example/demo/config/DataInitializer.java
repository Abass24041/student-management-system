package com.example.demo.config;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // Create permissions for student CRUD
        String[] permissions = {
                "list_student", "view_student", "create_student",
                "update_student", "delete_student"
        };

        for (String perm : permissions) {
            createPermissionIfNotFound(perm);
        }

        // Create admin role with all permissions
        createAdminRole("admin");

        // Create user role with read-only permissions
        createUserRole("user");

        // Create default admin user (username: admin, password: admin)
        createAdminUser("admin", "admin", "admin@demo.com");
    }

    @Transactional
    public void createPermissionIfNotFound(String name) {
        if (!permissionRepository.existsByName(name)) {
            Permission permission = new Permission();
            permission.setName(name);
            permissionRepository.save(permission);
        }
    }

    @Transactional
    public void createAdminRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
        }
        role.setPermissions(permissionRepository.findAll());
        roleRepository.save(role);
    }

    @Transactional
    public void createUserRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role.setPermissions(permissionRepository.findAll()
                    .stream()
                    .filter(p -> p.getName().startsWith("list_") || p.getName().startsWith("view_"))
                    .toList());
            roleRepository.save(role);
        }
    }

    @Transactional
    public void createAdminUser(String username, String password, String email) {
        Optional<AppUser> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setRole(roleRepository.findByName("admin"));
            userRepository.save(user);
        }
    }
}
