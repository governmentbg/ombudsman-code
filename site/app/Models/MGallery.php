<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Facades\DB;

/**
 * @property int    $ArG_id
 * @property int    $Ar_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $ArG_file
 * @property string $ArG_name
 * @property string $ArG_size
 * @property string $ArG_type
 */
class MGallery extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_gallery';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'ArG_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Ar_id', 'Mn_id', 'ArG_pin', 'ArG_file', 'ArG_name', 'ArG_size', 'ArG_type', 'created_at', 'updated_at', 'deleted_at'
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
        'ArG_id' => 'int', 'Ar_id' => 'int', 'Mn_id' => 'int',
        'ArG_pin' => 'int', 'ArG_file' => 'string', 'ArG_name' => 'string', 'ArG_size' => 'string', 'ArG_type' => 'string', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
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
    }

    // Scopes...

    // Functions ...

    public static function mediaList($lng, $key, $keyId, $all = false)
    {
        $mediaList =  DB::table('m_gallery as r')
            ->leftjoin('m_gallery_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.ArG_id', '=', 'r.ArG_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })




            // ->where('n18n.St_id', 1)
            ->where('r.' . $key, $keyId)

            ->whereNull('r.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->select(
                'r.*',
                'n18n.*',
                DB::raw("IF(SUBSTR(ArG_file, 1,4)='pub/',CONCAT('/',ArG_file),CONCAT('/pub/Gallery/',ArG_file))  as ArG_file"),
            )
            ->orderBy('r.ArG_pin', 'desc')
            ->orderBy('r.ArG_id', 'asc');

        if ($all) {
            $mediaList = $mediaList->get();
        } else {
            $mediaList = $mediaList->first();
        }

        return $mediaList;
    }

    // Relations ...
    public function eq_article()
    {
        return $this->belongsTo(MArticle::class, 'Ar_id');
    }

    public function eq_news()
    {
        return $this->belongsTo(MNews::class, 'Mn_id');
    }

    public function eq_lngGal()
    {
        return $this->hasMany(MGalleryLng::class, 'ArG_id');
    }
}
