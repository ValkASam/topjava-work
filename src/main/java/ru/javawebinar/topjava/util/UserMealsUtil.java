package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExceed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * GKislin
 * 31.05.2015.
 */
public class UserMealsUtil {
    public static void main(String[] args) {
        List<UserMeal> mealList = Arrays.asList(
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 29, 10, 0), "Завтрак", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 29, 11, 0), "Завтрак_2", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 29, 13, 0), "Обед", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 29, 20, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30, 10, 0), "Завтрак", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30, 13, 0), "Обед", 1500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30, 20, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 31, 13, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 31, 14, 0), "Ужин", 500)
        );
        getFilteredMealsWithExceeded(mealList, LocalTime.of(0, 0), LocalTime.of(23, 0), 2000);
//        .toLocalDate();
//        .toLocalTime();
    }

    public static List<UserMealWithExceed> getFilteredMealsWithExceeded(List<UserMeal> mealList, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO return List with correctly exceeded field
        List<UserMealWithExceed> result = new ArrayList<>();
        {
            //вариант 1: без stream
            result.clear();
            //фильтруем список съеденного по интервалу времени ...
            List<UserMeal> filteredList = new ArrayList<>();
            filteredList.addAll(mealList);
            Predicate<UserMeal> predicate = (userMeal) -> TimeUtil.isBetween(userMeal.getDateTime().toLocalTime(), startTime, endTime);
            filteredList.removeIf(predicate.negate()); //тут только подходящие под интервал записи
            //
            Map<LocalDate, Integer> cash = new HashMap<>(); //кеш - будет хранить даты, которые встретятся в filteredList, с суммой калорий за эту дату - чтобы не повторять проходы
            Function<LocalDate, Integer> dayCaloriesSumm = (k) -> { //функция для подсчета суммы калорий за дату
                final Integer[] calories = {0};
                mealList.forEach(um -> {
                    if (um.getDateTime().toLocalDate().isEqual(k)) {
                        calories[0] += um.getCalories();
                    }
                });
                return calories[0];
            };
            Consumer<UserMeal> consumer = (userMeal) -> { //функция для засовывания сеанса еды в List<UserMealWithExceed>
                LocalDate currDate = userMeal.getDateTime().toLocalDate(); //дата текущего сеанса еды
                Integer calories = cash.computeIfAbsent(currDate, dayCaloriesSumm); //дастаем из кеша сумму калорий за данную дату. Или расчитываем ее и кешируем
                boolean exceed = calories > caloriesPerDay; //true, если всего за день наедено больше лимита - присваиваем каждой записи из этого дня
                result.add(new UserMealWithExceed(userMeal.getDateTime(), userMeal.getDescription(), userMeal.getCalories(), exceed));
            };
            filteredList.forEach(consumer); //ходим по отфильтрованному списку и формируем List<UserMealWithExceed>
            //
        }
        {
            //вариант 2: используя stream  с предварительным маппингом сумм дневных калорий на даты
            result.clear();
            //карта калорий по дням:
            Map<LocalDate, Integer> dayCaloriesSummMap = mealList.stream().collect(Collectors.toMap((UserMeal um) -> um.getDateTime().toLocalDate(), UserMeal::getCalories, (c1, c2) -> c1 + c2));
            //
            Predicate<UserMeal> predicate = (userMeal) -> TimeUtil.isBetween(userMeal.getDateTime().toLocalTime(), startTime, endTime);
            mealList.stream().filter(predicate).forEach( //собираем в List<UserMealWithExceed>
                    (UserMeal um) -> result.add(new UserMealWithExceed(
                            um.getDateTime(),
                            um.getDescription(),
                            um.getCalories(),
                            dayCaloriesSummMap.get(um.getDateTime().toLocalDate()) > caloriesPerDay))
            );
        }
        return result;
    }
}
