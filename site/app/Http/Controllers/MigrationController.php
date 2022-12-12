<?php

namespace App\Http\Controllers;

use Carbon\Carbon;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class MigrationController extends Controller
{

    public function sync($catId, Request $request)
    {
        $data = DB::table('article')
            ->whereNull('synced')
            ->where('category_id', $catId)
            ->orderBy('id', 'asc')
            ->take($request->limit)
            ->get();



        foreach ($data as $n) {

            // $home = "проба";

            // dd(u8($n->anotation));
            // dd(u8($n->title)); 
            //  'created_at' => Carbon::now(),

            // news
            if ($catId == 1) {
                // 

                $mnId = DB::table('m_news')
                    ->insertGetId(
                        array(
                            'Mn_name' =>  u8($n->title),
                            'Mn_date' => $n->created,
                            'created_at' => $n->created,
                            'updated_at' => $n->created,
                        )
                    );
                // 18n
                $mnLid = DB::table('m_news_lng')
                    ->insertGetId(
                        array(
                            'Mn_id' =>  $mnId,
                            'S_Lng_id' => 1,
                            'MnL_path' => Str::slug(C2L(u8(substr($n->title, 0, 80)))) . '-' . $mnId,
                            'MnL_title' =>  u8($n->title),
                            'MnL_intro' => u8($n->anotation),
                            'MnL_body' => u8(fixTags($n->content)),
                            'St_id' => 1,
                            'created_at' => $n->created,
                            'updated_at' => $n->created,
                        )
                    );

                // images

                $picture = DB::table('picture_article as pa')
                    ->join('picture as p', 'p.id', '=', 'pa.picture_id')
                    ->where('article_id', $n->id)
                    ->get();



                foreach ($picture as $p) {

                    $mnGalId = DB::table('m_gallery')
                        ->insertGetId(
                            array(
                                'Mn_id' => $mnId,
                                'ArG_file' => 'pub/pictures/' . u8($p->name),
                                'ArG_name' => u8($p->caption),
                                'created_at' => $n->created,
                                'updated_at' => $n->created,
                            )
                        );

                    $mnGalLngId = DB::table('m_gallery_lng')
                        ->insertGetId(
                            array(
                                'ArG_id' => $mnGalId,
                                'S_Lng_id' => 1,
                                'ArGL_name' => u8($p->caption),
                                'created_at' => $n->created,
                                'updated_at' => $n->created,
                            )
                        );
                }


                $files = DB::table('document_article as pa')
                    ->join('document as p', 'p.id', '=', 'pa.document_id')
                    ->where('article_id', $n->id)
                    ->get();

                foreach ($files as $f) {

                    $arFId = DB::table('m_files')
                        ->insertGetId(
                            array(
                                'MnL_id' => $mnLid,
                                'ArF_file' => 'documents/' . trim(u8($f->name)),
                                'ArF_name' => u8($f->title),
                                'ArF_type' => 'file',
                                'created_at' => $n->created,
                                'updated_at' => $n->created,
                            )
                        );
                }


                // files
            } elseif ($catId == 2) {
                // 

                $fqId = DB::table('m_faq')
                    ->insertGetId(
                        array(
                            'Fq_name' =>  u8($n->title),
                            'Fq_type' =>  1,
                            'Fq_date' => $n->created,
                            'created_at' => $n->created,
                            'updated_at' => $n->created,
                        )
                    );
                // 18n
                $fqLid = DB::table('m_faq_lng')
                    ->insertGetId(
                        array(
                            'Fq_id' =>  $fqId,
                            'S_Lng_id' => 1,
                            'FqL_path' => Str::slug(C2L(u8(substr($n->title, 0, 80)))) . '-' . $fqId,
                            'FqL_title' =>  u8($n->title),

                            'FqL_body' =>  u8(fixTags($n->content)),
                            // 'FqL_body' =>  u8($n->anotation) . '<br/><br/>' . u8(fixTags($n->content)),
                            // 'St_id' => 1,
                            'created_at' => $n->created,
                            'updated_at' => $n->created,
                        )
                    );

                // images









            } elseif ($catId == 5) {
                // positions
                // 18n

                $files = DB::table('document_article as pa')
                    ->join('document as p', 'p.id', '=', 'pa.document_id')
                    ->where('article_id', $n->id)
                    ->first();
                if ($files) {
                    $Pst_file =
                        'pub/documents/' . u8($files->name);
                } else {
                    $Pst_file = '';
                }



                $pstId = DB::table('m_position')
                    ->insertGetId(
                        array(
                            'Pst_date' =>  $n->created,
                            'Pst_path' => Str::slug(C2L(u8(substr($n->title, 0, 80)))) . '-' . $n->id,
                            'Pst_name' =>  u8($n->title),
                            'Pst_body' => u8($n->content),
                            'Pst_doc_type' => 1,
                            'Pst_file' => $Pst_file,
                            'created_at' => $n->created,
                            'updated_at' => $n->created,
                        )
                    );
            } else {
                // various articles

                // update or insert
                if ($this->getAr($catId)["type"] == 'insert') {
                    $arId = DB::table('m_article')
                        ->insertGetId(
                            array(
                                'Ar_parent_id' => $this->getAr($catId)["relId"],
                                'Ar_type' => 1,
                                'Ar_menu' => 0,
                                'Ar_name' =>  u8($n->title),
                                'Ar_date' => $n->created,
                                'created_at' => $n->created,
                                'updated_at' => $n->created,
                            )
                        );
                    // 18n
                    $arLid = DB::table('m_article_lng')
                        ->insertGetId(
                            array(
                                'Ar_id' =>  $arId,
                                'S_Lng_id' => 1,
                                'ArL_path' => Str::slug(C2L(u8(substr($n->title, 0, 80)))) . '-' . $arId,
                                'ArL_title' =>  u8(fixTags($n->title)),
                                'ArL_intro' => u8(fixTags($n->anotation)),
                                'ArL_body' => u8(fixTags($n->content)),
                                'St_id' => 1,
                                'created_at' => $n->created,
                                'updated_at' => $n->created,
                            )
                        );

                    // images

                    $picture = DB::table('picture_article as pa')
                        ->join('picture as p', 'p.id', '=', 'pa.picture_id')
                        ->where('article_id', $n->id)
                        ->get();



                    foreach ($picture as $p) {

                        $arGalId = DB::table('m_gallery')
                            ->insertGetId(
                                array(
                                    'Ar_id' => $arId,
                                    'ArG_file' => 'pub/pictures/' . u8($p->name),
                                    'ArG_name' => u8($p->caption),
                                    'created_at' => $n->created,
                                    'updated_at' => $n->created,
                                )
                            );

                        $arGalLngId = DB::table('m_gallery_lng')
                            ->insertGetId(
                                array(
                                    'ArG_id' => $arGalId,
                                    'S_Lng_id' => 1,
                                    'ArGL_name' => u8($p->caption),
                                    'created_at' => $n->created,
                                    'updated_at' => $n->created,
                                )
                            );
                    }


                    // files

                    $files = DB::table('document_article as pa')
                        ->join('document as p', 'p.id', '=', 'pa.document_id')
                        ->where('article_id', $n->id)
                        ->get();

                    foreach ($files as $f) {

                        $arFId = DB::table('m_files')
                            ->insertGetId(
                                array(
                                    'ArL_id' => $arLid,
                                    'ArF_file' => 'documents/' . trim(u8($f->name)),
                                    'ArF_name' => u8($f->title),
                                    'ArF_type' => 'file',
                                    'created_at' => $n->created,
                                    'updated_at' => $n->created,
                                )
                            );
                    }
                } else {
                    // update related article
                }
            }



            DB::table('article')
                ->where('id', $n->id)
                ->update(['synced' => 1]);

            echo "updated <strong>{$n->id}</strong><hr/>";
        }
    }


    private function getAr($catId)
    {
        $details = [];
        if ($catId == "23") {
            $details =  [

                'type' => 'insert',
                'relId' => 10,


            ];
        } elseif ($catId == "22") {
            $details =  [

                'type' => 'insert',
                'relId' => 17,


            ];
        } elseif ($catId == "6") {
            // доклади
            $details =  [

                'type' => 'insert',
                'relId' => 55,


            ];
        } elseif ($catId == "20") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "17") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "16") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "13") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "12") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "11") {

            $details =  [

                'type' => 'insert',
                'relId' => 21,


            ];
        } elseif ($catId == "8") {

            $details =  [

                'type' => 'insert',
                'relId' => 43,


            ];
        } elseif ($catId == "26") {

            $details =  [

                'type' => 'insert',
                'relId' => 267,


            ];
        } elseif ($catId == "7") {

            $details =  [

                'type' => 'insert',
                'relId' => 274,


            ];
        } elseif ($catId == "9") {

            $details =  [

                'type' => 'insert',
                'relId' => 286,


            ];
        }

        return $details;
    }
}
