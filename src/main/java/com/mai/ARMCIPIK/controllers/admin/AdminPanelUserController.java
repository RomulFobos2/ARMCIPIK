package com.mai.ARMCIPIK.controllers.admin;

import com.mai.ARMCIPIK.models.Department;
import com.mai.ARMCIPIK.models.User;
import com.mai.ARMCIPIK.repo.DepartmentRepository;
import com.mai.ARMCIPIK.repo.UserRepository;
import com.mai.ARMCIPIK.service.OTPGenerator;
import com.mai.ARMCIPIK.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с пользователями
@Controller
public class AdminPanelUserController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping("/admin/allUsers")
    public String userList(Model model) {
        //Получаем пользователя, под которым выполнен вход (страница доступна только админу, соответсвенно пользователь будет только с ролью админа.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Получаем списко всех пользователей, для отображения на странице
        List<User> var_list = userService.allUsers();
        //Удаляем текущего пользователя из списка для отображения, чтобы он не мог удалить сам себя
        var_list.remove(userRepository.findByUsername(auth.getName()));
        //Оставляем только пользователей у которых имеется роль User.
        Iterable<User> users = var_list.stream().filter(x -> x.getRoles().stream().anyMatch(y -> y.getName().equals("ROLE_USER"))).collect(Collectors.toList());
        model.addAttribute("allUsers", users);
        return "admin/user/allUsers";
    }

    //Страниц для формы для добавления зам. по ВМР
    @GetMapping("/admin/addUser")
    public String addUser(Model model) {
        List<Department> allDepartments = departmentRepository.findAll();
        model.addAttribute("allDepartments", allDepartments);
        return "admin/user/addUser";
    }

    //Сохраняем менеджера
    @PostMapping("/admin/addUser")
    public String addUser(@RequestParam String userName, @RequestParam String inputLastName, @RequestParam String inputFirstName,
                          @RequestParam String inputPatronymicName, @RequestParam Long inputDepartment,
                          Model model) {
        if (userService.chekUserName(userName)) {
            model.addAttribute("userNameError", "Пользователь с таким именем уже существует");
            return "admin/user/addUser";
        }
        String oneTimePassword = OTPGenerator.getOneTimePassword();
        Department department = departmentRepository.findById(inputDepartment).orElseThrow();
        User user = new User(userName, oneTimePassword, oneTimePassword, inputLastName, inputFirstName, inputPatronymicName, department);
        userService.sendOneTimePasswordMail(userName, oneTimePassword, user);
        userService.saveUser(user, department.getDepartmentRole());
        return "redirect:/admin/allUsers";
    }

    //Формируем динамически страницу для каждого пользователя. Внутри страницы можно сделать операции над пользователем
    @GetMapping("/admin/allUsers/user-details/{id}")
    public String userDetails(@PathVariable(value = "id") long id, Model model) {
        if (!userRepository.existsById(id)) {
            return "redirect:/admin/allUsers";
        }
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("user", user);
        return "admin/user/user-details";
    }

    //Удаляем пользователя
    @PostMapping("/admin/allUsers/user-details/{id}/remove")
    public String delete_user(@PathVariable(value = "id") long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        userRepository.delete(user);
        return "redirect:/admin/allUsers";
    }

}
