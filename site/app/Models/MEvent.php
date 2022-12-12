<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Carbon\Carbon;

/**
 * @property int    $Mv_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $Mv_name
 * @property Date   $Mv_date
 */
class MEvent extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_event';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Mv_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Mv_name', 'Mv_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Mv_id' => 'int', 'Mv_name' => 'string', 'Mv_date' => 'datetime', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Mv_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });


        self::created(function ($article) {
            $truncated = substr($article->Mv_title, 0, 80);
            MEventLng::create([
                'Mv_id' => $article->Mv_id,
                'S_Lng_id' => 1,
                'MvL_path' =>  Str::slug(C2L($truncated)),
                'MvL_title' => $article->Mv_name,
            ]);
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->hasMany(MEventLng::class, 'Mv_id');
    }
    public function eq_stream()
    {
        return $this->hasMany(MStream::class, 'Mv_id');
    }

    // public function eq_gallery()
    // {
    //     return $this->hasMany(MGallery::class, 'Mv_id');
    // }



    public static function getEvents($lng, $limit = null)
    {
        // dd($slug);

        // App::setLocale($locale);
        // $lng = i18n($locale);


        $data =  DB::table('m_event as ev')
            ->join('m_event_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mv_id', '=', 'ev.Mv_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')
            ->leftjoin('m_stream as s', 's.Mv_id', '=', 'ev.Mv_id')



            ->where('n18n.St_id', 1)


            ->whereNull('ev.deleted_at')
            ->whereNull('n18n.deleted_at');

        $data->whereBetween('ev.Mv_date', [Carbon::now()->startOfWeek(), Carbon::now()->endOfWeek()]);
        // ->whereNull('ev.Mv_date')
        // ->select()
        if ($limit) {
            $data->take($limit);
        }
        $data->orderBy('Mv_date', 'asc');

        $data =  $data->get();

        foreach ($data as $key_parent => $n) {

            $files = MFiles::fileList('MvL_id', $n->MvL_id);


            $data[$key_parent]->files = $files;
        }




        // dd($data);


        return $data;
    }
}
