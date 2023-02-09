package com.mai.ARMCIPIK.service;

import com.mai.ARMCIPIK.models.Role;
import com.mai.ARMCIPIK.models.User;
import com.mai.ARMCIPIK.repo.RoleRepository;
import com.mai.ARMCIPIK.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

//Сервисный класс для сущности User.
@Service
public class UserService implements UserDetailsService {
    @PersistenceContext
    private EntityManager em;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private MailSender mailSender;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null & username.equals("admin")) {
            User userAdmin = new User("admin", "admin", "admin", "Администратор", "", "", null);
            saveAdmin(userAdmin);
            return userAdmin;
        }
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    public User findUserById(Long userId) {
        Optional<User> userFromDb = userRepository.findById(userId);
        return userFromDb.orElse(new User());
    }

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public boolean saveUser(User user, Role role) {
        User userFromDB = userRepository.findByUsername(user.getUsername());
        if (userFromDB != null) {
            return false;
        }
//        user.setRoles(Collections.singleton(new Role(1L, "ROLE_USER")));
        user.setRoles(new HashSet<Role>());
        user.getRoles().add(new Role(1L, "ROLE_USER"));
        user.getRoles().add(role);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setPasswordConfirm("");
        userRepository.save(user);
        return true;
    }

    public boolean saveAdmin(User user) {
        user.setRoles(new HashSet<Role>());
        user.getRoles().add(new Role(2L, "ROLE_ADMIN"));
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setPasswordConfirm("");
        userRepository.save(user);
        return true;
    }


    public boolean deleteUser(Long userId) {
        if (userRepository.findById(userId).isPresent()) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    //Проверяем свободно имя или нет.
    public boolean chekUserName(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        return true;
    }

    public void sendOneTimePasswordMail(String username, String oneTimePassword) {
        String message = String.format("Здравствуйте." + " Ваш одноразовый пароль: " + oneTimePassword);
        mailSender.send(username, "Код активации", message);
    }

    public void sendOneTimePasswordMail(String username, String oneTimePassword, User user) {
        String message = String.format("" + user.getFirstName() + " " + user.getPatronymicName() + ", здравствуйте." +
                "\nВам создана учетная запись." +
                "\nЛогин - " + username +
                "\nПароль: " + oneTimePassword +
                "\nНе забудьте сменить пароль!");
        mailSender.send(username, "Код активации", message);
    }

    public boolean haveRole(Long id, String checkRoleName){
        User user = userRepository.findById(id).orElseThrow();
        if (user != null){
            for(Role role : user.getRoles()){
                if (role.getName().equals(checkRoleName)){
                    return true;
                }
            }
        }
        return false;
    }

}