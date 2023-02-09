package com.mai.ARMCIPIK.controllers;

import org.springframework.stereotype.Controller;

/**
 * Контроллер для регистрации пользователей (не используется)
 */
@Controller
public class RegistrationController {
//    @Autowired
//    private UserService userService;
//
//    @GetMapping("/registration")
//    public String registration(Model model){
//        model.addAttribute(new User());
//        return "general/registration";
//    }
//
//    //Post запрос с получением введенных данных с формы регистрации.
//    @PostMapping("/registration")
//    public String addUser(@RequestParam String inputLastName, @RequestParam String inputFirstName, @RequestParam String inputPatronymicName,
//                          @RequestParam String username, @RequestParam String password, @RequestParam String passwordConfirm,
//                          Model model, HttpServletRequest request) {
//        //Создаем объект User используя данные с формы
//        User user = new User(username, password, passwordConfirm, inputLastName, inputFirstName, inputPatronymicName);
//        if(userService.chekUserName(username)){
//            model.addAttribute("usernameError", "Пользователь с таким именем уже существует");
//            return "general/registration";
//        }
//        if(!user.getPassword().equals(user.getPasswordConfirm())){
//            model.addAttribute("passwordError", "Пароли не совпадают");
//            return "general/registration";
//        }
//        String oneTimePassword = OTPGenerator.getOneTimePassword();
//        //Отправляем на почту (можно поменять на моб. телефон) сгенерированный выше код
//        userService.sendOneTimePasswordMail(username, oneTimePassword);
//        //userService.sendOneTimePasswordSMS(username, oneTimePassword);
//        //Сохраняем в данных текущей сесии имя пользоателя и сгенерированый код, чтобы можно было сравнить с кодом, который введет пользователь на странице /login-activate
//        request.getSession().setAttribute("user", user);
//        request.getSession().setAttribute("oneTimePassword", oneTimePassword);
//        return "redirect:/login-activate";
//    }
//
//    //Страница для ввода отпраленного на почту кода
//    @GetMapping("/login-activate")
//    public String user_activation(Model model, HttpServletRequest request){
//        User user = (User) request.getSession().getAttribute("user");
//        model.addAttribute("user", user);
//        return "general/login-activate";
//    }
//
//    @PostMapping("/login-activate")
//    public String chek_user_activation(@RequestParam String code, HttpServletRequest request, Model model){
//        User user = (User) request.getSession().getAttribute("user");
//        //Если пользователь ввел код отправленный ему на почту верно, то сохраняем его в БД
//        if(code.equals((String) request.getSession().getAttribute("oneTimePassword"))){
//            userService.saveUser(user);
//            return "redirect:/login";
//        }
//        else {
//            model.addAttribute("codeError", "Неверно введен код подтверждения");
//            //Почему то в model attribute теряется объект user с предудщей его подгрузки. Пришлось дополнительно добавлять его в этой части
//            model.addAttribute("user", user);
//            return "general/login-activate";
//        }
//    }
}