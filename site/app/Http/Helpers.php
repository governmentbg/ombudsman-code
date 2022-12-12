<?php

// use Illuminate\Http\Request;

use Illuminate\Support\Facades\DB;

function cleanUp($DataVars)
{
    $DataVars = trim(stripslashes(strip_tags(htmlspecialchars($DataVars, ENT_QUOTES))));
    return $DataVars;
}

function C2L($crz)
{

    $crz = str_replace("а", "a", $crz);
    $crz = str_replace("б", "b", $crz);
    $crz = str_replace("в", "v", $crz);
    $crz = str_replace("г", "g", $crz);
    $crz = str_replace("д", "d", $crz);
    $crz = str_replace("е", "e", $crz);
    $crz = str_replace("ж", "zh", $crz);
    $crz = str_replace("з", "z", $crz);
    $crz = str_replace("и", "i", $crz);
    $crz = str_replace("й", "y", $crz);
    $crz = str_replace("к", "k", $crz);
    $crz = str_replace("л", "l", $crz);
    $crz = str_replace("м", "m", $crz);
    $crz = str_replace("н", "n", $crz);
    $crz = str_replace("о", "o", $crz);
    $crz = str_replace("п", "p", $crz);
    $crz = str_replace("р", "r", $crz);
    $crz = str_replace("с", "s", $crz);
    $crz = str_replace("т", "t", $crz);
    $crz = str_replace("у", "u", $crz);
    $crz = str_replace("ф", "f", $crz);
    $crz = str_replace("х", "h", $crz);
    $crz = str_replace("ц", "ts", $crz);
    $crz = str_replace("ч", "ch", $crz);
    $crz = str_replace("ш", "sh", $crz);
    $crz = str_replace("щ", "sht", $crz);
    $crz = str_replace("ь", "y", $crz);
    $crz = str_replace("ъ", "a", $crz);
    $crz = str_replace("ю", "yu", $crz);
    $crz = str_replace("я", "ya", $crz);
    $crz = str_replace("А", "A", $crz);
    $crz = str_replace("Б", "B", $crz);
    $crz = str_replace("В", "V", $crz);
    $crz = str_replace("Г", "G", $crz);
    $crz = str_replace("Д", "D", $crz);
    $crz = str_replace("Е", "E", $crz);
    $crz = str_replace("Ж", "ZH", $crz);
    $crz = str_replace("З", "Z", $crz);
    $crz = str_replace("И", "I", $crz);
    $crz = str_replace("Й", "Y", $crz);
    $crz = str_replace("К", "K", $crz);
    $crz = str_replace("Л", "L", $crz);
    $crz = str_replace("М", "M", $crz);
    $crz = str_replace("Н", "N", $crz);
    $crz = str_replace("О", "O", $crz);
    $crz = str_replace("П", "P", $crz);
    $crz = str_replace("Р", "R", $crz);
    $crz = str_replace("С", "S", $crz);
    $crz = str_replace("Т", "T", $crz);
    $crz = str_replace("У", "U", $crz);
    $crz = str_replace("Ф", "F", $crz);
    $crz = str_replace("Х", "H", $crz);
    $crz = str_replace("Ц", "TS", $crz);
    $crz = str_replace("Ч", "CH", $crz);
    $crz = str_replace("Ш", "SH", $crz);
    $crz = str_replace("Щ", "SHT", $crz);
    $crz = str_replace("Ь", "Y", $crz);
    $crz = str_replace("Ъ", "A", $crz);
    $crz = str_replace("Ю", "YU", $crz);
    $crz = str_replace("Я", "YA", $crz);


    $crz = str_replace("January", "януари", $crz);
    $crz = str_replace("February", "февруари", $crz);
    $crz = str_replace("March", "март", $crz);
    $crz = str_replace("April", "април", $crz);
    $crz = str_replace("May", "май", $crz);
    $crz = str_replace("June", "юни", $crz);
    $crz = str_replace("July", "юли", $crz);
    $crz = str_replace("August", "август", $crz);
    $crz = str_replace("September", "септември", $crz);
    $crz = str_replace("October", "октомври", $crz);
    $crz = str_replace("November", "ноември", $crz);
    $crz = str_replace("December", "декември", $crz);
    $crz = str_replace("Monday", "понеделник", $crz);
    $crz = str_replace("Tuesday", "вторник", $crz);
    $crz = str_replace("Wednesday", "сряда", $crz);
    $crz = str_replace("Thursday", "четвъртък", $crz);
    $crz = str_replace("Friday", "петък", $crz);
    $crz = str_replace("Saturday", "събота", $crz);
    $crz = str_replace("Sunday", "неделя", $crz);

    return $crz;
}
function m2l($locale, $crz)
{
    if ($locale == 'bg') {

        $crz = str_replace("January", "януари", $crz);
        $crz = str_replace("February", "февруари", $crz);
        $crz = str_replace("March", "март", $crz);
        $crz = str_replace("April", "април", $crz);
        $crz = str_replace("May", "май", $crz);
        $crz = str_replace("June", "юни", $crz);
        $crz = str_replace("July", "юли", $crz);
        $crz = str_replace("August", "август", $crz);
        $crz = str_replace("September", "септември", $crz);
        $crz = str_replace("October", "октомври", $crz);
        $crz = str_replace("November", "ноември", $crz);
        $crz = str_replace("December", "декември", $crz);
        $crz = str_replace("Monday", "понеделник", $crz);
        $crz = str_replace("Tuesday", "вторник", $crz);
        $crz = str_replace("Wednesday", "сряда", $crz);
        $crz = str_replace("Thursday", "четвъртък", $crz);
        $crz = str_replace("Friday", "петък", $crz);
        $crz = str_replace("Saturday", "събота", $crz);
        $crz = str_replace("Sunday", "неделя", $crz);
    }


    return $crz;
}

function currentItem($value)
{
    return Request::segment($value);
}

function currentPath($value, $lng)

{
    // dd($value);
    $i = 0;
    $i2 = 0;

    if (Request::segment(1) == 'lr' or Request::segment(1) == 'prava-na-deteto') {
        $i = 1;
        $i2 = 3;
        $articlePath = Request::segment(4);
        $commonPrefix =  Request::segment(1) . "/";
        $articlePrefix = Request::segment(3) . "/";;
        $articleId = Request::segment(5);
    } else {
        $articlePath = Request::segment(3);
        $articlePrefix = "/" . Request::segment(2) . "/";
        $articleId = Request::segment(4);
        $commonPrefix = "";
    }

    $lang_array = array("en", "bg");
    if (in_array(Request::segment(1 + $i), $lang_array)) {
        $cleanPath = substr($value, 3 + $i2);
    } else {
        $cleanPath = $value;
    }

    // dd($articlePath);
    $newArticlePath = getPathById($lng, getIdBySegment($articlePath));
    // dd($newArticlePath);


    if (Request::segment(1) == 'lr' or Request::segment(1) == 'prava-na-deteto') {
        if (!Request::segment(3)) {
            $articlePrefix = '';
            $newArticlePath = '';
        }
    } else {
        if (!Request::segment(2)) {
            $articlePrefix = '';
            $newArticlePath = '';
        }
    }

    $cleanPath = "/{$commonPrefix}{$lng}{$articlePrefix}{$newArticlePath}";
    return  $cleanPath;
}

function i18n($value)
{

    $query = DB::table('s_lang')
        ->where('S_Lng_key', $value);

    $q = $query->exists();

    if ($q) {
        // dd($query->get());
        return $query->first()->S_Lng_id;
    } else {
        return 1;
    }
}

function dateToMs($value)
{
    $value = convertDateP($value, 1);


    return strtotime(date('Y-m-d 00:01:01', strtotime($value))) * 1000;
    // return strtotime(date('Y-m-d H:i:s', strtotime($value))) * 1000;
}

function lrSeg()
{
    $path = '';

    if (Request::segment(1) == 'lr' or Request::segment(1) == 'prava-na-deteto') {
        $path = '/' . Request::segment(1);
    }
    return $path;
}

function convertDateP($date, $fSec = null)
{
    list($d, $m, $Y) = explode("/", $date);
    $time = "";
    if ($fSec) {
        $time = date("G:i:s");
    }

    return "$Y-$m-$d" . " " . $time;
}

function u8($data)
{
    // return iconv("windows-1251", "utf-8//IGNORE", $data);
    return $data;
}

function fixTags($data)
{

    // 

    $search  = array('[justify]', '[/justify]', '[tab]', '[/tab]', '[left]', '[/left]', '[center]', '[/center]', '[img', '[/img]');
    $replace = array('', '', '', '');
    $string = str_replace($search, $replace, $data);

    return $string;

    // return strip_tags($string);
}


function fileAttType($value)
{
    if (strpos($value, '.doc')) {
        $icon = 'file-doc';
    } elseif (strpos($value, '.xls')) {
        $icon = 'file-excel';
    } elseif (strpos($value, '.txt')) {
        $icon = 'file';
    } elseif (strpos($value, '.ppt')) {
        $icon = 'file-ppt';
    } elseif (strpos($value, '.pdf')) {
        $icon = 'file-acrobat';
    } elseif (strpos($value, '.mp3')) {
        $icon = 'file-audio';
    } elseif (strpos($value, '.mp4')) {
        $icon = 'file-video';
    } elseif (strpos($value, '.mp4')) {
        $icon = 'file-video';
    } elseif (strpos($value, '.zip')) {
        $icon = 'file-archive';
    } elseif (strpos($value, '.rar')) {
        $icon = 'file-archive';
    } else {
        $icon = 'file';
    }

    return $icon;
}

function getIdBySegment($segment)
{
    $arId = DB::table('m_article_lng')
        ->where('ArL_path', '=', $segment)
        ->select('Ar_id')
        ->whereNull('deleted_at')
        ->first();

    if ($arId) {
        return $arId->Ar_id;
    } else {
        return 0;
    }
}


function getPathById($lng, $id)
{
    $lng = i18n($lng);
    // dd($lng);
    $arId = DB::table('m_article_lng')
        ->where('Ar_id', '=', $id)
        ->where('S_Lng_id', '=', $lng)
        ->select('ArL_path')
        ->whereNull('deleted_at')
        ->first();

    if ($arId) {
        return $arId->ArL_path;
    } else {
        return 0;
    }
}

function getConfig($lng, $id)
{
    $lng = i18n($lng);
    // dd($lng);
    $Cf_id = DB::table('m_config as n')
        ->leftjoin('m_config as n18n', function ($join) use ($lng) {
            $join->on('n18n.Cf_id', '=', 'n.Cf_id')
                ->where('n18n.S_Lng_id', '=',  $lng);
        })
        ->where('n.Cf_id', '=', $id)
        ->where('n.St_id', '=', 1)


        ->whereNull('n.deleted_at')
        ->first();

    if ($Cf_id) {
        return $Cf_id;
    } else {
        return 0;
    }
}

function getConfigGroup($lng, $groupId)
{
    $lng = i18n($lng);

    return DB::table('m_config as n')
        ->leftjoin('m_config as n18n', function ($join) use ($lng) {
            $join->on('n18n.Cf_id', '=', 'n.Cf_id')
                ->where('n18n.S_Lng_id', '=',  $lng);
        })
        ->where('n.CfT_id', '=', $groupId)
        ->where('n.St_id', '=', 1)


        ->whereNull('n.deleted_at')
        ->get();
}


function df($lng, $date, $time = null)
{
    $lng = i18n($lng);

    if ($time) {

        $date = Carbon\Carbon::createFromDate($date)->format('l, d.m.Y,  G:i');
    } else {
        $date = Carbon\Carbon::createFromDate($date)->format('l, d.m.Y');
    }


    if ($lng == 1) {
        $date = C2L($date);
        // $date = C2L($date) . " г.";
    }


    return $date;
}

function diffDates($date)
{

    // dd(trim(convertDateP($date1)));

    $current = \Carbon\Carbon::createFromFormat('Y-m-d', date('Y-m-d'));
    $to = \Carbon\Carbon::createFromFormat('Y-m-d', trim(convertDateP($date)));

    $diff_in_days = $to->diffInDays($current);



    // dd($diff_in_days);

    return $diff_in_days;
}

function futureDate($date)
{

    $current = \Carbon\Carbon::createFromFormat('Y-m-d', date('Y-m-d'));
    $to = \Carbon\Carbon::createFromFormat('Y-m-d', trim(convertDateP($date)));


    if ($to > $current) {

        return false;
    } else {

        return true;
    }
}

function claimKey()
{

    return date("Ymd-Gis") . "-" . rand(300, 9999);
}


function getConfigByKey($type = 1, $i18 = null,  $groupId, $keyId)
{
    if ($i18) {
        $lng = i18n($i18);
    }


    $data =  DB::table('m_config as n');
    if ($i18) {
        $data->leftjoin('m_config_lng as n18n', function ($join) use ($lng) {
            $join->on('n18n.Cf_id', '=', 'n.Cf_id')
                ->where('n18n.S_Lng_id', '=',  $lng);
        });
    }

    if ($groupId) {
        $data->where('n.CfT_id', '=', $groupId);
    }
    if ($keyId) {
        $data->where('n.Cf_id', '=', $keyId);
    }
    $data->where('n.St_id', '=', 1);

    if ($i18) {
        $data->whereNull('n18n.deleted_at');
    }

    if ($type == 1) {
        $data = $data->exists();
    } elseif ($type == 2) {
        $data = $data->first();
    } else {
        $data = $data->get();
    }

    return $data;
}
