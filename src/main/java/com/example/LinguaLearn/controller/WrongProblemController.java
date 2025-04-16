package com.example.LinguaLearn.controller;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.FirestoreService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Slf4j
@Controller
public class WrongProblemController {
    private final FirestoreService firestoreService;

    public WrongProblemController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    @GetMapping("/wrongNote")
    public String wrongNote(Model model, HttpSession httpSession) throws Exception {
        User user = (User) httpSession.getAttribute("user");
        List<String> wongNotes = firestoreService.getWrongProblem(user.getUid());

        log.info("오답노트 (영어 원문만): {}", wongNotes);
        model.addAttribute("wrongSentences", wongNotes);
        model.addAttribute("user", user);
        return "wrongNoteView";
    }
}
