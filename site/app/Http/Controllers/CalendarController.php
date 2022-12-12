<?php

namespace App\Http\Controllers;


use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;

class CalendarController extends Controller
{




    public static function archiveList($locale, $table, $index = 0, $index2 = 0)
    {

        App::setLocale($locale);
        $lng = i18n($locale);


        if (!self::getModel($table)) {
            $response = 'record not found';

            return $response;
        }



        $date = self::getModelDetails($table)["date_field"];
        $has_index = self::getModelDetails($table)["has_index"];
        $index_value = self::getModelDetails($table)["index_value"];
        $has_index2 = self::getModelDetails($table)["has_index2"];
        $index2_value = self::getModelDetails($table)["index2_value"];
        $id_field = self::getModelDetails($table)["id_field"];


        $table_alias = $table;
        $archiveList = DB::table("{$table} as t")

            ->select(DB::raw("YEAR(t.{$date}) as t_year"))
            ->whereRaw("YEAR(t.{$date})>0");
        if (self::getModelDetails($table)["has_18n"]) {

            $value_field = self::getModelDetails($table_alias)["value_18n"];
            $table_18n = self::getModelDetails($table)["table_18n"];
            $archiveList->join("{$table_18n} as t18n", function ($join) use ($lng, $id_field) {
                $join->on("t18n.{$id_field}", '=', "t.{$id_field}")
                    ->where('t18n.S_Lng_id', '=',  $lng);
            });

            $archiveList->whereNull("t18n.deleted_at");
        }

        if ($has_index) {

            $archiveList->where("t.{$index_value}", $index);
        }
        // private case ns_acts
        if ($table_alias == "m_news") {
            // $archiveList->where("t.St_id", '!=', 1);
        }


        if ($has_index2) {

            $archiveList->where("t.{$index2_value}", $index2);
        }

        $archiveList->orderBy('t_year', 'desc')
            ->distinct();
        $archiveList = $archiveList->get();



        $dateList = [];
        $monthList = [];
        foreach ($archiveList as $list) {

            $monthList = DB::table("{$table} as t")

                ->select(DB::raw("MONTH(t.{$date}) as t_month"))
                ->whereRaw("YEAR(t.{$date})>0")
                ->whereYear("t.{$date}", $list->t_year);

            $monthList->whereNull("t.deleted_at");

            if ($has_index) {

                $monthList->where("t.{$index_value}", $index);
            }

            if ($table_alias == "m_news") {
                // $monthList->whereNull("t.deleted_at");
            }



            $monthList->orderBy('t_month', 'asc')
                ->distinct();

            $monthList = $monthList->get();

            $dateList[] = [

                't_year' => $list->t_year,
                't_month_list' => $monthList,
            ];
        }






        $response = $dateList;
        return $response;
    }

    public static function archivePeriod($locale, $table, $year, $month, $index = 0, $index2 = 0)
    {

        // return 1;
        App::setLocale($locale);
        $lng = i18n($locale);

        if (!self::getModel($table)) {
            $response = 'Record not found';

            return $response;
        }


        $date = self::getModelDetails($table)["date_field"];
        $id_field = self::getModelDetails($table)["id_field"];
        $value_field = self::getModelDetails($table)["value_field"];
        $index_value = self::getModelDetails($table)["index_value"];
        $index2_value = self::getModelDetails($table)["index2_value"];
        $path_field = self::getModelDetails($table)["path_field"];
        $has_index = self::getModelDetails($table)["has_index"];
        $has_index2 = self::getModelDetails($table)["has_index2"];


        $table_alias = $table;

        $periodList = DB::table("{$table} as t");


        if (self::getModelDetails($table)["has_18n"]) {

            $value_field = self::getModelDetails($table_alias)["value_18n"];
            $table_18n = self::getModelDetails($table)["table_18n"];
            $periodList->join("{$table_18n} as t18n", function ($join) use ($lng, $id_field) {
                $join->on("t18n.{$id_field}", '=', "t.{$id_field}")
                    ->where('t18n.S_Lng_id', '=',  $lng);
            });

            $periodList->whereNull("t18n.deleted_at");
        }

        $periodList->whereNull("t.deleted_at");

        if ($has_index) {

            $periodList->where("t.{$index_value}", $index);
        }



        if ($table_alias == "m_news") {
            // $periodList->whereNull("t.deleted_at");
            // St_id, actual, m_events, past
        }



        if (self::getModelDetails($table)["has_index2"]) {

            $periodList->where("t.{$index2_value}", $index2);
        }



        $periodList->whereYear("t.{$date}", $year)
            ->whereMonth("t.{$date}", $month)
            ->select("t.{$id_field} as t_id", "{$value_field} as t_label", "t.{$date} as t_date", "{$path_field} as t_path")
            ->orderBy("t.{$date}", 'desc');
        $periodList = $periodList->get();
        $response = $periodList;
        return $response;
    }



    private static function getModel($target)
    {
        $models = array(
            "m_news", "m_article", "m_event", "m_position"
        );

        if (!in_array($target, $models)) {
            return false;
        } else {
            return true;
        }
    }



    private static function getModelDetails($target)
    {
        $details = [];
        if ($target == "m_news") {
            $details =  [

                'date_field' => 'Mn_date',
                'id_field' => 'Mn_id',
                'value_field' => '',
                'has_18n' => true,
                'table_18n' => 'm_news_lng',
                'value_18n' => 'MnL_title',
                'path_field' => 'MnL_path',
                'value_std' => '',
                'has_index' => true,
                'index_value' => 'Mn_type',
                'has_index2' => false,
                'index2_value' => '',

            ];
        } elseif ($target == "m_article") {
            $details =  [

                'date_field' => 'Ar_date',
                'id_field' => 'Ar_id',
                'value_field' => '',
                'has_18n' => true,
                'table_18n' => 'm_article_lng',
                'value_18n' => 'ArL_title',
                'path_field' => 'ArL_path',
                'value_std' => '',
                'has_index' => false,
                'index_value' => '',
                'has_index2' => false,
                'index2_value' => '',

            ];
        } elseif ($target == "m_event") {
            $details =  [

                'date_field' => 'Mv_date',
                'id_field' => 'Mv_id',
                'value_field' => '',
                'has_18n' => true,
                'table_18n' => 'm_event_lng',
                'value_18n' => 'MvL_title',
                'path_field' => 'MvL_path',
                'value_std' => '',
                'has_index' => false,
                'index_value' => '',
                'has_index2' => false,
                'index2_value' => '',

            ];
        } elseif ($target == "m_position") {
            $details =  [

                // 'date_field' => 'created_at',
                'date_field' => 'Pst_date',
                'id_field' => 'Pst_id',
                'value_field' => 'Pst_name',
                'has_18n' => false,
                'path_field' => 'Pst_path',
                'table_18n' => '',
                'value_18n' => '',
                'value_std' => '',
                'has_index' => false,
                'index_value' => 'Pst_doc_type',
                'has_index2' => false,
                'index2_value' => '',

            ];
        }

        return $details;
    }
}
