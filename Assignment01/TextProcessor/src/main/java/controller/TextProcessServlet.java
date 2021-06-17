package controller;

import com.google.gson.Gson;
import io.swagger.client.model.ErrMessage;
import io.swagger.client.model.ResultVal;
import io.swagger.client.model.TextLine;
import service.TextProcessor;
import service.WordCountService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TextProcessServlet extends HttpServlet {

    // this field used for store our various text processor
    private final Map<String, TextProcessor> functions = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        functions.put("/wordcount", new WordCountService());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset = UTF-8");

        PrintWriter out = response.getWriter();

        String pathVariable = request.getPathInfo();

        if (!isUrlValid(pathVariable)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ErrMessage errMessage = new ErrMessage().message("This operation is not provided");
            out.write(new Gson().toJson(errMessage));

        } else {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            TextLine textLine = new Gson().fromJson(sb.toString(), TextLine.class);

            TextProcessor processor = functions.get(pathVariable);

            // check if body exits and if the target is valid
            if (textLine != null
                    && processor.validate(textLine.getMessage())
                  ) {
                response.setStatus(HttpServletResponse.SC_OK);
                ResultVal result = new ResultVal().message(processor.apply(textLine.getMessage()));
//                ResultVal result = new ResultVal().message(1);
                out.write(new Gson().toJson(result));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ErrMessage errMessage = new ErrMessage().message("The input is not valid");
                out.write(new Gson().toJson(errMessage));
            }
        }
        out.flush();
        out.close();

    }

    /**
     * Check if the url is valid.
     * @param pathVariable the provided url and it is the function name in this case.
     * @return true if the url is valid and vice versa
     */
    private boolean isUrlValid(String pathVariable) {
        return functions.containsKey(pathVariable);
    }
}
