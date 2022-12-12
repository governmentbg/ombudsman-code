<?php

namespace App\Http\Controllers;

use App\Models\MFiles;
use App\Models\MGallery;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use App\Models\MNews;
use App\Models\MNewsLng;

class MNewsController extends Controller
{
    public static function countNews($locale)
    {

        App::setLocale($locale);
        $lng = i18n($locale);
        return DB::table('m_news as n')
            ->join('m_news_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mn_id', '=', 'n.Mn_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('m_gallery as g', 'g.Mn_id', '=', 'n.Mn_id')

            ->where('n18n.St_id', 1)
            ->where('n.Mn_type', 1)
            ->whereNull('n.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->skip(1)
            ->take(1)
            ->exists();
    }

    public static function frontNews($locale, $count, $skip, $Mn_type = 1, $frontImg = 1)
    {

        App::setLocale($locale);
        $lng = i18n($locale);

        // dd($lng);
        // dd(config("app.env"));
        // dd(config("app.url"));




        if ($Mn_type == 2) {
            $table = 'm_news';
        } else {
            $table = 'm_news';
        }
        $frontNews = DB::table($table . ' as n')
            ->join('m_news_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mn_id', '=', 'n.Mn_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            });

        if ($frontImg) {
            $frontNews->whereExists(function ($query) {
                $query->select(DB::raw(1))
                    ->from('m_gallery as img')
                    ->whereNull('img.deleted_at')
                    ->whereColumn('img.Mn_id', 'n.Mn_id');
            });
        }




        $frontNews->where('n18n.St_id', 1)
            ->where('n.Mn_type', $Mn_type)
            ->whereNull('n.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'n.Mn_date',
                'n.Mn_id',
                'n18n.MnL_title',
                'n18n.MnL_path',
                // 'img.Mn_id'
            )
            ->orderBy('n.Mn_pin', 'desc')
            ->orderBy('n.Mn_date', 'desc')
            ->orderBy('n.Mn_order', 'desc')
            ->orderBy('n.Mn_id', 'desc');

        if ($count == 1) {
            $frontNews = $frontNews->first();


            $frontNews->media = DB::table('m_gallery')
                ->select(
                    'ArG_id',
                    // 'G_Gal_file',
                    DB::raw("IF(SUBSTR(ArG_file, 1,4)='pub/',CONCAT('/',ArG_file),CONCAT('/pub/gallery/',ArG_file))  as ArG_file"),
                )
                ->where('Mn_id', $frontNews->Mn_id)
                ->whereNull('deleted_at')
                ->orderBy('ArG_pin', 'desc')
                ->orderBy('ArG_id', 'asc')

                ->first();
        } else {
            $frontNews = $frontNews->skip($skip)->take($count)->get();

            // if ($count != 1 && ) {
            //     dd(count($frontNews));
            // }
            if (count($frontNews) == 0) {

                $frontNews1 = $frontNews;
            }

            foreach ($frontNews as $list) {
                $media = DB::table('m_gallery')
                    ->select(
                        'ArG_id',
                        // 'G_Gal_file',
                        DB::raw("IF(SUBSTR(ArG_file, 1,4)='pub/',CONCAT('/',ArG_file),CONCAT('/pub/Gallery/',ArG_file))  as ArG_file"),
                    )
                    ->where('Mn_id', $list->Mn_id)
                    ->whereNull('deleted_at')
                    ->orderBy('ArG_pin', 'desc')
                    ->orderBy('ArG_id', 'asc')

                    ->first();


                $frontNews1[] = [

                    'Mn_date' => $list->Mn_date,
                    'Mn_id' => $list->Mn_id,
                    'MnL_title' => $list->MnL_title,
                    'MnL_path' => $list->MnL_path,
                    'media' => $media,
                ];
            }
            $frontNews = $frontNews1 ?? '';
        }
        // if ($count != 1) {
        //     dd($frontNews);
        // }

        // if ($Mn_type == 2) {
        //     dd($frontNews1);
        // }


        return $frontNews;
    }

    public function getNews($locale, $slug, $Mn_type = 2)
    {
        // dd($slug);

        App::setLocale($locale);
        $lng = i18n($locale);
        $id = $slug;

        // dd($locale);

        $data =  DB::table('m_news as art')
            ->join('m_news_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mn_id', '=', 'art.Mn_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')



            ->where('n18n.St_id', 1)
            // ->where('art.Mn_type', $Mn_type)
            ->where('n18n.MnL_path', $id)

            ->whereNull('art.deleted_at')
            ->whereNull('n18n.deleted_at')
            // ->select()

            ->first();

        // dd($data);



        if (!$data) {
            // pass to 404
            return view('content.404', [
                'data' => '',
            ]);
        } else {



            // Files

            $data->files = MFiles::fileList('MnL_id', $data->MnL_id);
            // Gallery
            $data->cover = MGallery::mediaList($lng, 'Mn_id', $data->Mn_id);
            $data->gallery = MGallery::mediaList($lng, 'Mn_id', $data->Mn_id, true);
            // dd($data->gallery);

            // Sub arrays


            return view('content.news', [
                // 'data' => $response,
                'data' => $data,
                // 'data' => $response,
                // 'stack' => $stack,

            ]);
        }
    }
}
