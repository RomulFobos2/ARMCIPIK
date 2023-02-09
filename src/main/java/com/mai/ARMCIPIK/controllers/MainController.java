package com.mai.ARMCIPIK.controllers;

import com.mai.ARMCIPIK.models.Role;
import com.mai.ARMCIPIK.models.User;
import com.mai.ARMCIPIK.repo.RoleRepository;
import com.mai.ARMCIPIK.repo.UserRepository;
import com.mai.ARMCIPIK.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;


/**
 * Контроллер для отображения страниц доступных без авторизации и общих страниц
 */
@Controller
public class MainController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        User currentUser = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if(currentUser != null && currentUser.isNeedChangePass()){
            return "redirect:/change-password";
        }
        model.addAttribute("title", "Главная страница");
        return "general/home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        initialSetting();
        model.addAttribute("title", "Вход");
        return "general/login";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        //На страницу загружаем объект "Пользователь" который эту страницу посещает
        User currentUser = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        String roleName = currentUser.getRoles().stream().toList().get(0).getName();
        switch (roleName){
            case "ROLE_USER" : roleName = "Пользователь"; break;
            default: roleName = "Техническая учетная запись";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("roleName", roleName);
        return "general/profile";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        return "admin/admin";
    }

    @GetMapping("/user")
    public String user(Model model) {
        User currentUser = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("currentUser", currentUser);
        return "user/user";
    }

    @GetMapping("/change-password")
    public String changePassword(HttpServletRequest request, Model model) {
        User currentUser = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (request.getSession().getAttribute("passwordError") != null && (Boolean) request.getSession().getAttribute("passwordError")) {
            model.addAttribute("passwordError", "Пароли не совпадают. Потдвердите новый пароль верно");
            request.getSession().setAttribute("passwordError", false);
        }
        model.addAttribute("currentUser", currentUser);
        return "general/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String passwordNew, @RequestParam String passwordConfirm,
                                 HttpServletRequest request, Model model) {
        //Получаем текущего пользователя, который инициировал смену пароля
        User currentUser = userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        //С помощью функции matches определяем, что пользователь ввел свой текущий пароль верно
        if (passwordNew.equals(passwordConfirm)) {
            currentUser.setPassword(bCryptPasswordEncoder.encode(passwordNew));
            currentUser.setNeedChangePass(false);
            userRepository.save(currentUser);
        }
        else {
            request.getSession().setAttribute("passwordError", true);
            return "redirect:/change-password";
        }
        return "redirect:/logout";
    }

    //Первоначальная настройка приложения. Создание ролей, админа.
    public void initialSetting(){
        if(roleRepository.count() == 0){
            Role roleUser = new Role(1L, "ROLE_USER");
            Role roleAdmin = new Role(2L, "ROLE_ADMIN");
            roleRepository.save(roleUser);
            roleRepository.save(roleAdmin);
        }
        User user = userRepository.findByUsername("admin");
        if (user == null){
            User userAdmin = new User("admin", "admin", "admin", "Администратор", "", "", null);
            userService.saveAdmin(userAdmin);
        }
    }









}