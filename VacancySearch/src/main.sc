require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /

    state: Start
        q!: $regex</start>
        a: Начнём.
        intent: /привет || toState = "/Hello"
        event: noMatch || toState = "/NoMatch"

    state: Hello
        intent!: /привет
        a: Привет привет
        intent: /jobSearch || toState = "/запрос профессии"
        event: noMatch || toState = "./"

    state: запрос города
        a: В каком городе вы хотите найти работу?
        buttons:
            "Не указывать" -> /Обновление города
        intent: /jobRequest || toState = "/Определение города"
        event: noMatch || toState = "./"

    state: Определение города
        intent: /jobRequest
        script:
            $session.city = $parseTree._City;
        if: $session.city == undefined
            a: Я не понял. Вы сказали: {{$request.query}}
            go!: /запрос города
        else: 
            a: ваш город {{$session.city}}
            go!: /запрос зарплаты
        event: noMatch || toState = "./"

    state: Bye
        intent!: /пока
        a: Пока пока

    state: NoMatch
        event!: noMatch
        a: Я не понял. Вы сказали: {{$request.query}}
        if: $session.city==undefined || $session.city!=undefined
            go!: /Start

    state: Match
        event!: match
        a: {{$context.intent.answer}}

    state: Определение зарплаты
        intent!: /salaryRequest
        script:
            $session.salary = $parseTree._Salary;
        if: $session.salary == undefined
            a: Я не понял. Вы сказали: {{$request.query}}
            go!: /запрос зарплаты
        else: 
            a: вы выбрали зарплату {{$session.salary}}
            go!: /Запрос параметра
        event: noMatch || toState = "/Инфо"

    state: NewState
        script:
            $temp.response = $http.post(
                "http://185.242.118.144:8000/find_jobs", 
                {
            body: {
                text: $session.city + " " + $session.profession,
                salary: $session.salary
            },
            headers: {
                "Content-Type": "application/json"
            }
                }
            );
        # Отправляем запрос на внешний API для поиска вакансий
        if: $temp.response.isOk && $temp.response.data.length > 0
            script:
                $temp.vacancyMessages = "";
                $temp.index = 0;
                while ($temp.index < $temp.response.data.length) {
                    $temp.vacancyMessages += "---\n";
                    $temp.vacancyMessages += "Профессия: " + $temp.response.data[$temp.index].position + "\n";
                    $temp.vacancyMessages += "Компания: " + $temp.response.data[$temp.index].company + "\n";
                    $temp.vacancyMessages += "Город: " + $temp.response.data[$temp.index].location + "\n";
                    $temp.vacancyMessages += "Зарплата: от " + $temp.response.data[$temp.index].from_salary + 
                " до " + $temp.response.data[$temp.index].to_salary + 
                " " + $temp.response.data[$temp.index].currency + "\n";
                    $temp.vacancyMessages += "Ссылка: " + $temp.response.data[$temp.index].link + "\n";
                    $temp.index++;
                }
            a: |
                Вот несколько вакансий для тебя:
                {{$temp.vacancyMessages}}
        else: 
            # Если вакансии не найдены или произошла ошибка
            a: Не удалось найти вакансий по вашим параметрам. Попробуй ещё раз.

    state: запрос профессии
        a: Какая профессия вас интересует?
        buttons:
            "Не указывать" -> /обновление профессии
        intent: /professionRequest || toState = "/определение профессии"
        event: noMatch || toState = "./"

    state: определение профессии
        intent!: /professionRequest
        script:
            $session.profession = $parseTree._Profession;
        if: $session.profession == undefined
            go!: /запрос профессии
        else: 
            a: Вы выбрали профессию {{$session.profession}}
            go!: /запрос города
        event: noMatch || toState = "./"

    state: запрос зарплаты
        a: Какую зарплату вы хотите?
        buttons:
            "Не указывать" -> /Обновление зп и запрос параметра
        intent: /salaryRequest || toState = "/Определение зарплаты"
        event: noMatch || toState = "./"

    state: Обновление зп и запрос параметра
        script:
            $session.salary = undefined;
        if: $session.city == "" && $session.profession == undefined && $session.salary == undefined
            a: Необходимо указать хотя-бы 1 параметр
            buttons:
                "В начало" -> /запрос профессии
        else: 
            go!: /Подтверждение данных
        event: noMatch || toState = "/Инфо"

    state: обновление профессии
        script:
            $session.profession = $parseTree._Profession;
        if: $session.profession == undefined
            go!: /запрос города

    state: Обновление города
        script:
            $session.city = $parseTree._City;
            $session.city = ""
        if: $session.city == ""
            go!: /запрос зарплаты

    state: Подтверждение данных
        a: Ваши данные:
                Профессия: {{$session.profession}}
                Город: {{$session.city}}
                Зарплата: {{$session.salary}}
                Подтвердите поиск или начните заново
        buttons:
            "Подтвердить" -> /NewState
            "Начать с начала" -> /запрос профессии

    state: Запрос параметра
        if: $session.city == undefined && $session.profession == undefined && $session.salary == undefined
            a: Необходимо указать хотя-бы 1 параметр
            buttons:
                "В начало" -> /запрос профессии
        else: 
            go!: /Подтверждение данных
        event: noMatch