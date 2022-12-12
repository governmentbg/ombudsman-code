<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class NavigationController extends Controller
{
    public static function navListParent($lng, $parentId)
    {

        $list =   DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')


            ->where('nav.Ar_parent_id', $parentId)
            // ->where('nav.Ar_type', 1)
            // ->where('nav.Ar_id', '!=', $currentId)
            // ->whereNull('nav.Ar_menu')
            ->where('n18n.St_id', 1)
            ->where('nav.Ar_menu', '!=', 1)
            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'n18n.ArL_path',
                'nav.Ar_id',
                'n18n.ArL_title',
                'nav.Ar_date',

                'lng.S_Lng_key',


            )
            ->orderBy('nav.Ar_order', 'desc')
            ->orderBy('nav.Ar_id', 'desc');


        $list = $list->get();
        // dd($list);


        return $list;
    }
    public static function navListCurrent($lng, $currentId, $count = 0)
    {
        // select current parent
        $Ar_parent_id = DB::table('m_article')
            ->select('Ar_parent_id')
            ->where('Ar_id', $currentId)
            ->first()->Ar_parent_id;

        $list =   DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')


            ->where('nav.Ar_parent_id', $Ar_parent_id)
            ->where('nav.Ar_type', 1)
            // ->where('nav.Ar_id', '!=', $currentId)
            ->where('n18n.St_id', 1)
            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'n18n.ArL_path',
                'nav.Ar_id',
                'n18n.ArL_title',

                'lng.S_Lng_key',


            )
            ->orderBy('Ar_order', 'desc');

        if ($count) {
            $list = $list->count();
        } else {
            $list = $list->get();
        }

        return $list;
    }

    public static function navList($locale, $type)
    {

        // return $locale;

        $lng = i18n($locale);

        $nav = self::navObject($lng,  NULL, $type);

        // $new_nav = $nav;
        foreach ($nav as $key_parent => $n) {

            $nav1 = self::navObject($lng,  $n->Ar_id, $type);

            if ($nav1->isEmpty()) {
                // $nav[$key_parent]->sub_nav = [];
            } else {
                foreach ($nav1 as $key_parent1 => $n1) {

                    $nav2 = self::navObject($lng,  $n1->Ar_id, $type);

                    if ($nav2->isEmpty()) {
                        // $nav1[$key_parent1]->sub_nav = [];
                    } else {
                        $nav1[$key_parent1]->sub_nav = $nav2;
                    }


                    foreach ($nav2 as $key_child_1 => $n2) {

                        $nav3 = self::navObject($lng,  $n2->Ar_id, $type);
                        if ($nav3->isEmpty()) {
                            // $nav1[$key_parent1]->sub_nav = [];
                        } else {
                            $nav2[$key_child_1]->sub_nav = $nav3;
                        }
                    }
                }
                $nav[$key_parent]->sub_nav = $nav1;
            }
        }

        // dd($nav);

        return $nav;
    }


    public static function navObject($lng, $parent, $type, $menu = 1)
    {




        $nav =  DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')


            ->where('nav.Ar_parent_id', $parent)
            ->where('nav.Ar_type', $type);
        if (!$menu) {
            $nav->where('nav.Ar_menu', '!=', 1);
        } else {
            $nav->where('nav.Ar_menu', '=', 1);
        }

        $nav->where('n18n.St_id', 1)
            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'n18n.ArL_path',
                'nav.Ar_id',
                'n18n.ArL_title',
                'n18n.ArL_url',
                'nav.Ar_icon',

                'lng.S_Lng_key',
                DB::raw("(SELECT COUNT(*) FROM m_article as ma 
                inner join m_article_lng as mal on mal.Ar_id = ma.Ar_id and mal.S_Lng_id = '$lng'
                WHERE ma.Ar_parent_id = nav.Ar_id and mal.St_id=1 and ma.Ar_menu=1) as subCount")

            )
            ->orderBy('Ar_order', 'desc');

        return $nav = $nav->get();
    }


    public static function metaData($lng, $string)
    {
        $lng = i18n($lng);



        $meta = DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')
            ->where('n18n.ArL_path', $string)
            ->where('n18n.St_id', 1)
            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'n18n.ArL_path',
                'nav.Ar_parent_id',
                'nav.Ar_id',
                'n18n.ArL_title',
                'lng.S_Lng_key'
            )
            ->first();


        if ($meta) {
            // $meta->path = [];
            $meta->sub = DB::table('m_article as nav')
                ->join('m_article_lng as n18n', function ($join) use ($lng) {
                    $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                        ->where('n18n.S_Lng_id', '=',  $lng);
                })
                ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')

                ->where('nav.Ar_parent_id', $meta->Ar_id)
                ->where('n18n.St_id', 1)
                ->whereNull('nav.deleted_at')
                ->whereNull('n18n.deleted_at')
                ->select(
                    'n18n.ArL_path',
                    'nav.Ar_id',
                    'n18n.ArL_title',
                    // 'n18n.C_NavL_URL',
                    'lng.S_Lng_key'
                )
                ->get();
            $meta->parent = DB::table('m_article as nav')
                ->join('m_article_lng as n18n', function ($join) use ($lng) {
                    $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                        ->where('n18n.S_Lng_id', '=',  $lng);
                })
                ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')

                ->where('nav.Ar_id', $meta->Ar_parent_id)
                ->where('n18n.St_id', 1)
                ->whereNull('nav.deleted_at')
                ->whereNull('n18n.deleted_at')
                // ->whereNotNull('nav.Ar_parent_id')
                ->select(
                    'n18n.ArL_path',
                    'nav.Ar_id',
                    'nav.Ar_parent_id',
                    'n18n.ArL_title',
                    'lng.S_Lng_key'
                )
                ->first();

            $meta->parent_cn = ($meta->parent) ? 1 : 0;

            // dd($meta);
        } else {
            $meta = [
                'ArL_path' => '404',
                'Ar_id' => '0',
                'ArL_title' => '',
                'S_Lng_key' => 'bg',
            ];
        }

        // dd($meta);

        // $response = Response::json($meta, 200);
        return $meta;
    }
}
