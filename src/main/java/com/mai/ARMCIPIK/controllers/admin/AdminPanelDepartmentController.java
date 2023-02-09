package com.mai.ARMCIPIK.controllers.admin;


import com.mai.ARMCIPIK.models.Department;
import com.mai.ARMCIPIK.models.Role;
import com.mai.ARMCIPIK.repo.DepartmentRepository;
import com.mai.ARMCIPIK.repo.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminPanelDepartmentController {
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/admin/allDepartments")
    public String allDepartment(Model model, HttpServletRequest request) {
        if (request.getSession().getAttribute("deleteError") != null && (Boolean) request.getSession().getAttribute("deleteError")) {
            model.addAttribute("deleteError", true);
            request.getSession().setAttribute("deleteError", false);
        }
        List<Department> allDepartments = departmentRepository.findAll();
        model.addAttribute("allDepartments", allDepartments);
        return "admin/department/allDepartments";
    }

    @GetMapping("/admin/allDepartments/department-details/{id}")
    public String departmentDetails(@PathVariable(value = "id") long id, Model model) {
        if (!departmentRepository.existsById(id)) {
            return "redirect:/admin/allDepartments";
        }
        Department department = departmentRepository.findById(id).orElseThrow();
        model.addAttribute("department", department);
        String line_separator = System.getProperty("line.separator");
        model.addAttribute("line_separator", line_separator);


        return "admin/department/department-details";
    }

    @PostMapping("/admin/allDepartments/department-details/{id}/remove")
    public String delete_department(@PathVariable(value = "id") long id,
                                    Model model, HttpServletRequest request) {
        Department department = departmentRepository.findById(id).orElseThrow();
        try {
            departmentRepository.delete(department);
        } catch (Exception e) {
            request.getSession().setAttribute("deleteError", true);
        }
        return "redirect:/admin/allDepartments";
    }

    @GetMapping("/admin/admin-addDepartment")
    public String addDepartment(Model model) {
        return "admin/department/addDepartment";
    }

    @PostMapping("/admin/addDepartment")
    public String addDepartment(@RequestParam String inputDepartment, @RequestParam String inputDepartmentRole, @RequestParam String inputAddress, @RequestParam String inputEmail,
                                @RequestParam String inputTel, @RequestParam String inputDescription, @RequestParam String inputShortDescription, Model model) {
        //Добавляем уникальное значение к каждой роли, чтобы не выполнять проверку на наличие имени роли в БД.
        inputDepartmentRole = "ROLE_DEP" + inputDepartmentRole.toUpperCase() + "_" + java.util.UUID.randomUUID().toString().split("-")[0];
        Role departmentRole = new Role(inputDepartmentRole);
        Department department = new Department(inputDepartment, inputAddress, inputEmail, inputTel, inputDescription, inputShortDescription, departmentRole);
        Department departmentFromDB = departmentRepository.findByName(inputDepartment);
        if (departmentFromDB != null) {
            model.addAttribute("departmentNameError", "Данное структурное подразделение уже существует");
            return "admin/department/addDepartment";
        }
        roleRepository.save(departmentRole);
        departmentRepository.save(department);
        return "redirect:/admin/allDepartments";
    }


    @GetMapping("/admin/editDepartment/{id}")
    public String departmentEdit(@PathVariable(value = "id") long id, Model model) {
        if (!departmentRepository.existsById(id)) {
            return "redirect:/admin/allDepartments";
        }
        Department department = departmentRepository.findById(id).orElseThrow();
        model.addAttribute("department", department);
        return "admin/department/editDepartment";
    }

    //Пост запрос для сохранения информации о департаменте
    @PostMapping("/admin/editDepartment/{id}")
    public String departmentEdit(@PathVariable(value = "id") long id,
                                @RequestParam String inputDepartment, @RequestParam String inputAddress,
                                @RequestParam String inputEmail,@RequestParam String inputTel,
                                @RequestParam String inputDescription, @RequestParam String inputShortDescription,
                                Model model) {
        Department department = departmentRepository.findById(id).orElseThrow();
        department.setName(inputDepartment);
        department.setAddress(inputAddress);
        department.setEmail(inputEmail);
        department.setNumber(inputTel);
        department.setDescription(inputDescription);
        department.setShortDescription(inputShortDescription);
        departmentRepository.save(department);
        return "redirect:/admin/allDepartments/department-details/{id}";
    }
}